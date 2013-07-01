package com.traviswyatt.ioio.adxl345;

import ioio.lib.api.IOIO;
import ioio.lib.api.SpiMaster;
import ioio.lib.api.SpiMaster.Rate;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends IOIOActivity {

	private TextView ioioStatusText;
	private TextView deviceIdText;
	private TextView xAxisText;
	private TextView yAxisText;
	private TextView zAxisText;
	private TextView magnitudeText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		ioioStatusText = (TextView) findViewById(R.id.ioio_status);
		deviceIdText = (TextView) findViewById(R.id.device_id);
		xAxisText = (TextView) findViewById(R.id.x_axis);
		yAxisText = (TextView) findViewById(R.id.y_axis);
		zAxisText = (TextView) findViewById(R.id.z_axis);
		magnitudeText = (TextView) findViewById(R.id.magnitude);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	@Override
	protected IOIOLooper createIOIOLooper() {
		int sdoPin = 29; // miso
		int sdaPin = 28; // mosi
		int sclPin = 27; // clk
		int csPin  = 30; // slave select
		Rate rate = SpiMaster.Rate.RATE_2M;
		final ADXL345 adxl345 = new ADXL345(sdoPin, sdaPin, sclPin, csPin, rate);
		adxl345.setListener(new ADXL345.ADXL345Listener() {
			@Override
			public void onDeviceId(byte deviceId) {
				updateTextView(deviceIdText, "Device ID: " + (int) (deviceId & 0xFF));
			}
			@Override
			public void onData(int x, int y, int z) {
				float x_axis = x * adxl345.getMultiplier() * 9.8f;
				float y_axis = y * adxl345.getMultiplier() * 9.8f;
				float z_axis = z * adxl345.getMultiplier() * 9.8f;
				double magnitude = Math.sqrt(x_axis * x_axis + y_axis * y_axis + z_axis * z_axis);
				updateTextView(xAxisText, "X = " + x_axis);
				updateTextView(yAxisText, "Y = " + y_axis);
				updateTextView(zAxisText, "Z = " + z_axis);
				updateTextView(magnitudeText, "Magnitude = " + magnitude);
			}
			@Override
			public void onError(String message) {
				// TODO Auto-generated method stub
			}
		});
		return new DeviceLooper(adxl345);
	}
	
	private void updateTextView(final TextView textView, final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				textView.setText(text);
			}
		});
	}
	
	/**
	 * This is the thread on which all the IOIO activity happens. It will be run
	 * every time the application is resumed and aborted when it is paused. The
	 * method setup() will be called right after a connection with the IOIO has
	 * been established (which might happen several times!). Then, loop() will
	 * be called repetitively until the IOIO gets disconnected.
	 */
	class DeviceLooper implements IOIOLooper {
		
		/**
		 * Duration to sleep after each loop.
		 */
		private static final long THREAD_SLEEP = 10L; // milliseconds
		
		private IOIOLooper device;

		public DeviceLooper(IOIOLooper device) {
			this.device = device;
		}
		
		@Override
		public void setup(IOIO ioio) throws ConnectionLostException, InterruptedException {
			device.setup(ioio);
			updateTextView(ioioStatusText, "Connected");
		}

		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * @throws InterruptedException 
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 */
		@Override
		public void loop() throws ConnectionLostException, InterruptedException {
			device.loop();
			Thread.sleep(THREAD_SLEEP);
		}

		@Override
		public void disconnected() {
			device.disconnected();
			updateTextView(ioioStatusText, "Disconnected");
		}

		@Override
		public void incompatible() {
			device.incompatible();
			updateTextView(ioioStatusText, "Incompatible");
		}
	}

}
