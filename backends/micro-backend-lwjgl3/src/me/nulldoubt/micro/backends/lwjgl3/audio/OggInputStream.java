package me.nulldoubt.micro.backends.lwjgl3.audio;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.utils.Streams;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class OggInputStream extends InputStream {
	
	private final static int BUFFER_SIZE = 512;
	byte[] buffer;
	int bytes = 0;
	boolean bigEndian = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);
	boolean endOfBitStream = true;
	boolean inited = false;
	private int convsize = BUFFER_SIZE * 4;
	private final byte[] convbuffer;
	private final InputStream input;
	private final Info oggInfo = new Info(); // struct that stores all the static vorbis bitstream settings
	private boolean endOfStream;
	private final SyncState syncState = new SyncState(); // sync and verify incoming physical bitstream
	private final StreamState streamState = new StreamState(); // take physical pages, weld into a logical stream of packets
	private final Page page = new Page(); // one Ogg bitstream page. Vorbis packets are inside
	private final Packet packet = new Packet(); // one raw packet of data for decode
	private final Comment comment = new Comment(); // struct that stores all the bitstream user comments
	private final DspState dspState = new DspState(); // central working state for the packet->PCM decoder
	private final Block vorbisBlock = new Block(dspState); // local working space for packet->PCM decode
	private int readIndex;
	private final ByteBuffer pcmBuffer;
	private final int total;
	
	public OggInputStream(InputStream input) {
		this(input, null);
	}
	
	public OggInputStream(InputStream input, OggInputStream previousStream) {
		if (previousStream == null) {
			convbuffer = new byte[convsize];
			pcmBuffer = BufferUtils.createByteBuffer(4096 * 500);
		} else {
			convbuffer = previousStream.convbuffer;
			pcmBuffer = previousStream.pcmBuffer;
		}
		
		this.input = input;
		try {
			total = input.available();
		} catch (IOException ex) {
			throw new MicroRuntimeException(ex);
		}
		
		init();
	}
	
	/**
	 * Get the number of bytes on the stream
	 *
	 * @return The number of the bytes on the stream
	 */
	public int getLength() {
		return total;
	}
	
	public int getChannels() {
		return oggInfo.channels;
	}
	
	public int getSampleRate() {
		return oggInfo.rate;
	}
	
	/**
	 * Initialise the streams and thread involved in the streaming of OGG data
	 */
	private void init() {
		initVorbis();
		readPCM();
	}
	
	/**
	 * Initialise the vorbis decoding
	 */
	private void initVorbis() {
		syncState.init();
	}
	
	/**
	 * Get a page and packet from that page
	 *
	 * @return True if there was a page available
	 */
	private boolean getPageAndPacket() {
		// grab some data at the head of the stream. We want the first page
		// (which is guaranteed to be small and only contain the Vorbis
		// stream initial header) We need the first page to get the stream
		// serialno.
		
		// submit a 4k block to libvorbis' Ogg layer
		int index = syncState.buffer(BUFFER_SIZE);
		if (index == -1)
			return false;
		
		buffer = syncState.data;
		if (buffer == null) {
			endOfStream = true;
			return false;
		}
		
		try {
			bytes = input.read(buffer, index, BUFFER_SIZE);
		} catch (Exception e) {
			throw new MicroRuntimeException("Failure reading Vorbis.", e);
		}
		syncState.wrote(bytes);
		
		// Get the first page.
		if (syncState.pageout(page) != 1) {
			// have we simply run out of data? If so, we're done.
			if (bytes < BUFFER_SIZE)
				return false;
			
			// error case. Must not be Vorbis data
			throw new MicroRuntimeException("Input does not appear to be an Ogg bitstream.");
		}
		
		// Get the serial number and set up the rest of decode.
		// serialno first; use it to set up a logical stream
		streamState.init(page.serialno());
		
		// extract the initial header from the first page and verify that the
		// Ogg bitstream is in fact Vorbis data
		
		// I handle the initial header first instead of just having the code
		// read all three Vorbis headers at once because reading the initial
		// header is an easy way to identify a Vorbis bitstream and it's
		// useful to see that functionality separated out.
		
		oggInfo.init();
		comment.init();
		if (streamState.pagein(page) < 0) {
			// error; stream version mismatch perhaps
			throw new MicroRuntimeException("Error reading first page of Ogg bitstream.");
		}
		
		if (streamState.packetout(packet) != 1) {
			// no page? must not be vorbis
			throw new MicroRuntimeException("Error reading initial header packet.");
		}
		
		if (oggInfo.synthesis_headerin(comment, packet) < 0) {
			// error case; not a vorbis header
			throw new MicroRuntimeException("Ogg bitstream does not contain Vorbis audio data.");
		}
		
		// At this point, we're sure we're Vorbis. We've set up the logical
		// (Ogg) bitstream decoder. Get the comment and codebook headers and
		// set up the Vorbis decoder
		
		// The next two packets in order are the comment and codebook headers.
		// They're likely large and may span multiple pages. Thus we read
		// and submit data until we get our two packets, watching that no
		// pages are missing. If a page is missing, error out; losing a
		// header page is the only place where missing data is fatal. */
		
		int i = 0;
		while (i < 2) {
			while (i < 2) {
				int result = syncState.pageout(page);
				if (result == 0)
					break; // Need more data
				// Don't complain about missing or corrupt data yet.
				// We'll catch it at the packet output phase
				
				if (result == 1) {
					streamState.pagein(page); // we can ignore any errors here
					// as they'll also become apparent at packetout
					while (i < 2) {
						result = streamState.packetout(packet);
						if (result == 0)
							break;
						if (result == -1) {
							// Uh oh; data at some point was corrupted or missing!
							// We can't tolerate that in a header. Die.
							throw new MicroRuntimeException("Corrupt secondary header.");
						}
						
						oggInfo.synthesis_headerin(comment, packet);
						i++;
					}
				}
			}
			// no harm in not checking before adding more
			index = syncState.buffer(BUFFER_SIZE);
			if (index == -1)
				return false;
			buffer = syncState.data;
			try {
				bytes = input.read(buffer, index, BUFFER_SIZE);
			} catch (Exception e) {
				throw new MicroRuntimeException("Failed to read Vorbis.", e);
			}
			if (bytes == 0 && i < 2) {
				throw new MicroRuntimeException("End of file before finding all Vorbis headers.");
			}
			syncState.wrote(bytes);
		}
		
		convsize = BUFFER_SIZE / oggInfo.channels;
		
		// OK, got and parsed all three headers. Initialize the Vorbis packet->PCM decoder.
		dspState.synthesis_init(oggInfo); // central decode state
		vorbisBlock.init(dspState); // local state for most of the decode
		// so multiple block decodes can proceed in parallel.
		// We could init multiple vorbis_block structures for vd here
		
		return true;
	}
	
	/**
	 * Decode the OGG file as shown in the jogg/jorbis examples
	 */
	private void readPCM() {
		boolean wrote = false;
		
		while (true) { // we repeat if the bitstream is chained
			if (endOfBitStream) {
				if (!getPageAndPacket()) {
					break;
				}
				endOfBitStream = false;
			}
			
			if (!inited) {
				inited = true;
				return;
			}
			
			float[][][] _pcm = new float[1][][];
			int[] _index = new int[oggInfo.channels];
			// The rest is just a straight decode loop until end of stream
			while (!endOfBitStream) {
				while (!endOfBitStream) {
					int result = syncState.pageout(page);
					
					if (result == 0) {
						break; // need more data
					} else if (result == -1) { // missing or corrupt data at this page position
						Micro.app.error("gdx-audio", "Error reading OGG: Corrupt or missing data in bitstream.");
					} else {
						streamState.pagein(page); // can safely ignore errors at this point
						while (true) {
							result = streamState.packetout(packet);
							
							if (result == 0)
								break; // need more data
							if (result != -1) {
								// we have a packet. Decode it
								int samples;
								if (vorbisBlock.synthesis(packet) == 0) { // test for success!
									dspState.synthesis_blockin(vorbisBlock);
								}
								
								// **pcm is a multichannel float vector. In stereo, for
								// example, pcm[0] is left, and pcm[1] is right. samples is
								// the size of each channel. Convert the float values
								// (-1.<=range<=1.) to whatever PCM format and write it out
								
								while ((samples = dspState.synthesis_pcmout(_pcm, _index)) > 0) {
									float[][] pcm = _pcm[0];
									// boolean clipflag = false;
									int bout = (samples < convsize ? samples : convsize);
									
									// convert floats to 16 bit signed ints (host order) and interleave
									for (int i = 0; i < oggInfo.channels; i++) {
										int ptr = i * 2;
										// int ptr=i;
										int mono = _index[i];
										for (int j = 0; j < bout; j++) {
											int val = (int) (pcm[i][mono + j] * 32767.);
											// might as well guard against clipping
											if (val > 32767) {
												val = 32767;
											}
											if (val < -32768) {
												val = -32768;
											}
											if (val < 0)
												val = val | 0x8000;
											
											if (bigEndian) {
												convbuffer[ptr] = (byte) (val >>> 8);
												convbuffer[ptr + 1] = (byte) (val);
											} else {
												convbuffer[ptr] = (byte) (val);
												convbuffer[ptr + 1] = (byte) (val >>> 8);
											}
											ptr += 2 * (oggInfo.channels);
										}
									}
									
									int bytesToWrite = 2 * oggInfo.channels * bout;
									if (bytesToWrite > pcmBuffer.remaining()) {
										throw new MicroRuntimeException(
												"Ogg block too big to be buffered: " + bytesToWrite + " :: " + pcmBuffer.remaining());
									} else {
										pcmBuffer.put(convbuffer, 0, bytesToWrite);
									}
									
									wrote = true;
									dspState.synthesis_read(bout); // tell libvorbis how many samples we actually consumed
								}
							}
						}
						if (page.eos() != 0) {
							endOfBitStream = true;
						}
						
						if ((!endOfBitStream) && (wrote)) {
							return;
						}
					}
				}
				
				if (!endOfBitStream) {
					bytes = 0;
					int index = syncState.buffer(BUFFER_SIZE);
					if (index >= 0) {
						buffer = syncState.data;
						try {
							bytes = input.read(buffer, index, BUFFER_SIZE);
						} catch (Exception e) {
							throw new MicroRuntimeException("Error during Vorbis decoding.", e);
						}
					} else {
						bytes = 0;
					}
					syncState.wrote(bytes);
					if (bytes == 0) {
						endOfBitStream = true;
					}
				}
			}
			
			// clean up this logical bitstream; before exit we see if we're
			// followed by another [chained]
			streamState.clear();
			
			// ogg_page and ogg_packet structs always point to storage in
			// libvorbis. They're never freed or manipulated directly
			
			vorbisBlock.clear();
			dspState.clear();
			oggInfo.clear(); // must be called last
		}
		
		// OK, clean up the framer
		syncState.clear();
		endOfStream = true;
	}
	
	public int read() {
		if (readIndex >= pcmBuffer.position()) {
			((Buffer) pcmBuffer).clear();
			readPCM();
			readIndex = 0;
		}
		if (readIndex >= pcmBuffer.position()) {
			return -1;
		}
		
		int value = pcmBuffer.get(readIndex);
		if (value < 0) {
			value = 256 + value;
		}
		readIndex++;
		
		return value;
	}
	
	public int read(byte[] b) {
		return read(b, 0, b.length);
	}
	
	public int read(byte[] b, int off, int len) {
		for (int i = 0; i < len; i++) {
			int value = read();
			if (value >= 0) {
				b[i] = (byte) value;
			} else {
				if (i == 0) {
					return -1;
				} else {
					return i;
				}
			}
		}
		
		return len;
	}
	
	/**
	 * @see java.io.InputStream#available()
	 */
	public int available() {
		return endOfStream ? 0 : 1;
	}
	
	public void close() {
		Streams.closeQuietly(input);
	}
	
	public boolean atEnd() {
		return endOfStream && (readIndex >= pcmBuffer.position());
	}
	
}
