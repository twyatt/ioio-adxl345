package com.traviswyatt.ioio.adxl345;

import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.SpiMaster;
import ioio.lib.api.SpiMaster.Rate;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;

public class ADXL345 implements IOIOLooper {
	
	public static final Rate DEFAULT_RATE = Rate.RATE_250K;
	public static final Range DEFAULT_RANGE = Range.RANGE_16G;
	
	/**
	 * Bit structure:
	 * | Read/Write | Multibyte | A5 | A4 | A3 | A2 | A1 | A0 |
	 */
	public static final byte ADXL345_SPI_WRITE  = (byte) 0x00; // 0000 0000
	public static final byte ADXL345_SPI_READ   = (byte) 0x80; // 1000 0000
	public static final byte ADXL345_MULTI_BYTE = (byte) 0x40; // 0100 0000
	
	/**
	 * Register map per datasheet.
	 * 
	 * http://www.analog.com/static/imported-files/data_sheets/ADXL345.pdf
	 */
	public static final byte DEVID          = (byte) 0x00; // Device ID
	public static final byte THRESH_TAP     = (byte) 0x1D; // Tap threshold
	public static final byte OFFSX          = (byte) 0x1E; // X-axis offset
	public static final byte OFFSY          = (byte) 0x1F; // Y-axis offset
	public static final byte OFFSZ          = (byte) 0x20; // Z-axis offset
	public static final byte DUR            = (byte) 0x21; // Tap duration
	public static final byte Latent         = (byte) 0x22; // Tap latency
	public static final byte Window         = (byte) 0x23; // Tap window
	public static final byte THRESH_ACT     = (byte) 0x24; // Activity threshold
	public static final byte THRESH_INACT   = (byte) 0x25; // Inactivity threshold
	public static final byte TIME_INACT     = (byte) 0x26; // Inactivity time
	public static final byte ACT_INACT_CTL  = (byte) 0x27; // Axis enable control for activity and inactivity detection
	public static final byte THRESH_FF      = (byte) 0x28; // Free-fall threshold
	public static final byte TIME_FF        = (byte) 0x29; // Free-fall time
	public static final byte TAP_AXES       = (byte) 0x2A; // Axis control for single tap/double tap
	public static final byte ACT_TAP_STATUS = (byte) 0x2B; // Source of single tap/double tap
	public static final byte BW_RATE        = (byte) 0x2C; // Data rate and power mode control
	public static final byte POWER_CTL      = (byte) 0x2D; // Power-saving features control
	public static final byte INT_ENABLE     = (byte) 0x2E; // Interrupt enable control
	public static final byte INT_MAP        = (byte) 0x2F; // Interrupt mapping control
	public static final byte INT_SOURCE     = (byte) 0x30; // Source of interrupts
	public static final byte DATA_FORMAT    = (byte) 0x31; // Data format control
	public static final byte DATAX0         = (byte) 0x32; // X-Axis Data 0
	public static final byte DATAX1         = (byte) 0x33; // X-Axis Data 1
	public static final byte DATAY0         = (byte) 0x34; // Y-Axis Data 0
	public static final byte DATAY1         = (byte) 0x35; // Y-Axis Data 1
	public static final byte DATAZ0         = (byte) 0x36; // Z-Axis Data 0
	public static final byte DATAZ1         = (byte) 0x37; // Z-Axis Data 1
	public static final byte FIFO_CTL       = (byte) 0x38; // FIFO control
	public static final byte FIFO_STATUS    = (byte) 0x39; // FIFO status
	
	public static final byte DEFAULT_RESET_VALUE    = (byte) 0x00;
	public static final byte DEVID_RESET_VALUE      = (byte) 0xE5;
	public static final byte BW_RATE_RESET_VALUE    = (byte) 0x0A;
	public static final byte INT_SOURCE_RESET_VALUE = (byte) 0x02;
	
	public static final byte BW_RATE_3200_Hz = (byte) 0x0F; // 0000 1111
	public static final byte BW_RATE_1600_Hz = (byte) 0x0E; // 0000 1110
	public static final byte BW_RATE_800_Hz  = (byte) 0x0D; // 0000 1101
	public static final byte BW_RATE_400_Hz  = (byte) 0x0C; // 0000 1100
	public static final byte BW_RATE_200_Hz  = (byte) 0x0B; // 0000 1011
	public static final byte BW_RATE_100_Hz  = (byte) 0x0A; // 0000 1010
	public static final byte BW_RATE_50_Hz   = (byte) 0x09; // 0000 1001
	public static final byte BW_RATE_25_Hz   = (byte) 0x08; // 0000 1000
	// ...
	
	/**
	 * Register 0x2C-BW_RATE (Read/Write)
	 * 
	 * | D7 | D6 | D5 |     D4    | D3 | D2 | D1 | D0 |
	 * |  0 |  0 |  0 | LOW_POWER |        Rate       |
	 */
	public static final byte BW_RATE_Rate = (byte) 0x0F; // 0000 1111
	
	/**
	 * Register 0x2D-POWER_CTL (Read/Write)
	 * 
	 * | D7 | D6 |  D5  |     D4     |   D3    |   D2  | D1 | D0 |
	 * |  0 |  0 | Link | AUTO_SLEEP | Measure | Sleep |  Wakeup |
	 */
	public static final byte POWER_CTL_Measure = (byte) 0x08; // 0000 1000
	
	/**
	 * Register 0x31-DATA_FORMAT (Read/Write)
	 * 
	 * |     D7    |  D6 |     D5     | D4 |    D3    |    D2   | D1 | D0 |
	 * | SELF_TEST | SPI | INT_INVERT |  0 | FULL_RES | Justify |  Range  |
	 */
	public static final byte DATA_FORMAT_SELF_TEST = (byte) 0x80; // 0100 0000
	public static final byte DATA_FORMAT_SPI_3WIRE = (byte) 0x40; // 0100 0000
	
	/**
	 * Associates G range values with their corresponding DATA_FORMAT bits.
	 */
	public enum Range {
		RANGE_2G  (2f,  (byte) 0x00), // +/- 2 G
		RANGE_4G  (4f,  (byte) 0x01), // +/- 4 G
		RANGE_8G  (8f,  (byte) 0x02), // +/- 8 G
		RANGE_16G (16f, (byte) 0x03)  // +/- 16 G
		;
		private final float value;
		private final byte bits; // DATA_FORMAT's D1 and D0 bits
		Range(float value, byte bits) {
			this.value = value;
			this.bits = bits;
		}
	}
	
	private static final int BUFFER_SIZE = 10; // bytes
	
	public interface ADXL345Listener {
		public void onDeviceId(byte deviceId);
		public void onData(int x, int y, int z);
		public void onError(String message);
	}
	
	private ADXL345Listener listener;
	
	private int x;
	private int y;
	private int z;
	
	private final byte[] buffer = new byte[BUFFER_SIZE];
	
	private final DigitalInput.Spec miso;
	private final DigitalOutput.Spec mosi;
	private final DigitalOutput.Spec clk;
	private final DigitalOutput.Spec[] slaveSelect;
	private final Rate rate;
	private Range range;
	
	private SpiMaster spi;
	
	public ADXL345(int sdoPin, int sdaPin, int sclPin, int csPin) {
		this(sdoPin, sdaPin, sclPin, csPin, DEFAULT_RATE);
	}
	
	public ADXL345(int sdoPin, int sdaPin, int sclPin, int csPin, Rate rate) {
		this(sdoPin, sdaPin, sclPin, csPin, rate, DEFAULT_RANGE);
	}
	
	public ADXL345(int sdoPin, int sdaPin, int sclPin, int csPin, Rate rate, Range range) {
		miso = new DigitalInput.Spec(sdoPin);
		mosi = new DigitalOutput.Spec(sdaPin);
		clk  = new DigitalOutput.Spec(sclPin);
		slaveSelect = new DigitalOutput.Spec[] { new DigitalOutput.Spec(csPin) };
		this.rate = rate;
		this.range = range;
	}
	
	public ADXL345 setListener(ADXL345Listener listener) {
		this.listener = listener;
		return this;
	}
	
	private void setupDevice() throws InterruptedException, ConnectionLostException {
		byte deviceId = readDeviceId();
		if (deviceId != DEVID_RESET_VALUE) {
			onError("Invalid device ID, expected " + (DEVID_RESET_VALUE & 0xFF) + " but got " + (deviceId & 0xFF));
		}
		if (listener != null) {
			listener.onDeviceId(deviceId);
		}
		
		write(DATA_FORMAT, range.bits);
		write(POWER_CTL, POWER_CTL_Measure);
		write(BW_RATE, BW_RATE_1600_Hz);
	}
	
	/**
	 * Returns the multiplier that should be used to convert data values to Gs.
	 * 
	 * @return
	 */
	public float getMultiplier() {
		return range.value * 2f / 1024f;
	}
	
	public byte readDeviceId() throws ConnectionLostException, InterruptedException {
		read(DEVID, 1);
		return buffer[0];
	}
	
	/**
	 * Writes specified register and value.
	 * 
	 * @param register
	 * @param value
	 * @throws ConnectionLostException
	 * @throws InterruptedException
	 */
	protected void write(byte register, byte value) throws ConnectionLostException, InterruptedException {
		buffer[0] = register;
		buffer[1] = value;
		flush(2);
	}
	
	/**
	 * Writes specified register and values.
	 * 
	 * @param register
	 * @param values
	 * @throws ConnectionLostException
	 * @throws InterruptedException
	 */
	protected void write(byte register, byte[] values) throws ConnectionLostException, InterruptedException {
		buffer[0] = register;
		System.arraycopy(values, 0, buffer, 1, values.length);
		flush(1 + values.length);
	}
	
	/**
	 * Writes the buffer to the SPI.
	 * 
	 * @param length Number of bytes of the buffer to write.
	 * @throws ConnectionLostException
	 * @throws InterruptedException
	 */
	protected void flush(int length) throws ConnectionLostException, InterruptedException {
		int writeSize = length;
		int readSize = 0;
		int totalSize = writeSize + readSize;
		spi.writeRead(buffer, writeSize, totalSize, buffer, readSize);
	}
	
	protected void read(byte register, int length) throws ConnectionLostException, InterruptedException {
	    byte tx = (byte) (register | ADXL345_SPI_READ);
	    if (length > 1) // multi-byte read
	    	tx |= ADXL345_MULTI_BYTE;
	    buffer[0] = tx;
		
		int writeSize = 1;
		int readSize = length;
		int totalSize = writeSize + readSize;
		spi.writeRead(buffer, writeSize, totalSize, buffer, readSize);
	}
	
	private void onError(String message) {
		if (listener != null) {
			listener.onError(message);
		}
	}
	
	/*
	 * IOIOLooper interface methods.
	 */

	@Override
	public void setup(IOIO ioio) throws ConnectionLostException, InterruptedException {
		boolean invertClk = true;
		boolean sampleOnTrailing = true;
		SpiMaster.Config config = new SpiMaster.Config(rate, invertClk, sampleOnTrailing);
		spi = ioio.openSpiMaster(miso, mosi, clk, slaveSelect, config);
		
		setupDevice();
	}

	@Override
	public void loop() throws ConnectionLostException, InterruptedException {
		if (listener != null) {
			read(DATAX0, 6);
			x = (buffer[1] << 8) | buffer[0];
			y = (buffer[3] << 8) | buffer[2];
			z = (buffer[5] << 8) | buffer[4];
			listener.onData(x, y, z);
		}
	}

	@Override
	public void disconnected() {
		// TODO Auto-generated method stub
	}

	@Override
	public void incompatible() {
		// deprecated
	}

	@Override
	public void incompatible(IOIO ioio) {
		// TODO Auto-generated method stub
	}
	
}
