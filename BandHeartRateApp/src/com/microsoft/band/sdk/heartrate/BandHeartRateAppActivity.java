//Copyright (c) Microsoft Corporation All rights reserved.  
// 
//MIT License: 
// 
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
//documentation files (the  "Software"), to deal in the Software without restriction, including without limitation
//the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
//to permit persons to whom the Software is furnished to do so, subject to the following conditions: 
// 
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of
//the Software. 
// 
//THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
//TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
//THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
//CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.
package com.microsoft.band.sdk.heartrate;

import java.io.DataOutputStream;
import java.lang.ref.WeakReference;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.SampleRate;

import com.microsoft.band.sensors.BandAltimeterEvent;
import com.microsoft.band.sensors.BandAltimeterEventListener;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class BandHeartRateAppActivity extends Activity {

	private BandClient client = null;
	private Button btnStart, btnConsent;
	private TextView txtStatus;
	private TextView gsrStatus ,heartRateStatus , heartRateQualityStatus;
	private TextView accelReadings;


	private String output="";
	private String accel ="";
	private String url_link	;
    private String  heartRateQuality ;
    private int heartRate ,gsrValue ;
	private float accelX , accelY,accelZ;
	private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
            	output = (String.format("Heart Rate = %d beats per minute\n"
            			+ "Quality = %s\n +" ,heartRate= event.getHeartRate(), heartRateQuality=event.getQuality().toString()));
            }
        }
    };

	private BandGsrEventListener mGsrEventListener = new BandGsrEventListener() {
		@Override
		public void onBandGsrChanged(final BandGsrEvent event) {
			if (event != null) {
				output+=(String.format("Resistance = %d kOhms\n", gsrValue = event.getResistance()));
				//appendToUI(String.format("Time = %d \n", event.getTimestamp()));
			}
			appendToUI(output);

		}
	};

	private BandAccelerometerEventListener mAccelerometerEventListener = new BandAccelerometerEventListener() {
		@Override
		public void onBandAccelerometerChanged(final BandAccelerometerEvent event) {
			if (event != null) {
				accel=(String.format(" X = %.3f \n Y = %.3f\n Z = %.3f",accelX= event.getAccelerationX(),
						accelY=event.getAccelerationY(), accelZ=event.getAccelerationZ()));

			}
		}
	};

	private BandAltimeterEventListener mAltimeterEventListener = new BandAltimeterEventListener() {
		@Override
		public void onBandAltimeterChanged(final BandAltimeterEvent event) {
			if (event != null) {
				appendToUI(new StringBuilder().append(String.format("Total Gain = %d cm\n", event.getTotalGain()))
						.append(String.format("Total Loss = %d cm\n", event.getTotalLoss()))
						.append(String.format("Stepping Gain = %d cm\n", event.getSteppingGain()))
						.append(String.format("Stepping Loss = %d cm\n", event.getSteppingLoss()))
						.append(String.format("Steps Ascended = %d\n", event.getStepsAscended()))
						.append(String.format("Steps Descended = %d\n", event.getStepsDescended()))
						.append(String.format("Rate = %f cm/s\n", event.getRate()))
						.append(String.format("Flights of Stairs Ascended = %d\n", event.getFlightsAscended()))
						.append(String.format("Flights of Stairs Descended = %d\n", event.getFlightsDescended())).toString());
			}
		}
	};




	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtStatus = (TextView) findViewById(R.id.textStatus);
		gsrStatus = (TextView) findViewById(R.id.textIp);
		heartRateStatus =(TextView)findViewById(R.id.textHeartRate);
		heartRateQualityStatus =(TextView)findViewById(R.id.textHeartRateQuality);
		accelReadings = (TextView) findViewById(R.id.textAccel);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				txtStatus.setText("");
				new HeartRateSubscriptionTask().execute();
				new GsrSubscriptionTask().execute();
				new AccelerometerSubscriptionTask().execute();
				url_link = gsrStatus.getText().toString();

			}
		});
        
        final WeakReference<Activity> reference = new WeakReference<Activity>(this);
        
        btnConsent = (Button) findViewById(R.id.btnConsent);
        btnConsent.setOnClickListener(new OnClickListener() {
			@SuppressWarnings("unchecked")
            @Override
			public void onClick(View v) {
				new HeartRateConsentTask().execute(reference);
			}
		});
    }
	
	@Override
	protected void onResume() {
		super.onResume();
		txtStatus.setText("");
	}
	
    @Override
	protected void onPause() {
		super.onPause();
		if (client != null) {
			try {
				client.getSensorManager().unregisterHeartRateEventListener(mHeartRateEventListener);
			} catch (BandIOException e) {
				appendToUI(e.getMessage());
			}
		}
	}
	
    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }
    
	private class HeartRateSubscriptionTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
				if (getConnectedBandClient()) {
					if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
						client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
					} else {
						appendToUI("You have not given this application consent to access heart rate data yet."
								+ " Please press the Heart Rate Consent button.\n");
					}
				} else {
					appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
				}
			} catch (BandException e) {
				String exceptionMessage="";
				switch (e.getErrorType()) {
				case UNSUPPORTED_SDK_VERSION_ERROR:
					exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
					break;
				case SERVICE_ERROR:
					exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
					break;
				default:
					exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
					break;
				}
				appendToUI(exceptionMessage);

			} catch (Exception e) {
				appendToUI(e.getMessage());
			}
			return null;
		}
	}
	
	private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
		@Override
		protected Void doInBackground(WeakReference<Activity>... params) {
			try {
				if (getConnectedBandClient()) {
					
					if (params[0].get() != null) {
						client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
							@Override
							public void userAccepted(boolean consentGiven) {
							}
					    });
					}
				} else {
					appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
				}
			} catch (BandException e) {
				String exceptionMessage="";
				switch (e.getErrorType()) {
				case UNSUPPORTED_SDK_VERSION_ERROR:
					exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
					break;
				case SERVICE_ERROR:
					exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
					break;
				default:
					exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
					break;
				}
				appendToUI(exceptionMessage);

			} catch (Exception e) {
				appendToUI(e.getMessage());
			}
			return null;
		}
	}
	
	private void appendToUI(final String string ) {
		this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	txtStatus.setText(string);
				accelReadings.setText(accel);
				gsrStatus.setText(gsrValue);
				heartRateStatus.setText(heartRate);
				heartRateQualityStatus.setText(heartRateQuality);

            }
        });

	}



	private class GsrSubscriptionTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
				if (getConnectedBandClient()) {
					int hardwareVersion = Integer.parseInt(client.getHardwareVersion().await());
					if (hardwareVersion >= 20) {
						appendToUI("Band is connected.\n");
						client.getSensorManager().registerGsrEventListener(mGsrEventListener);
					} else {
						appendToUI("The Gsr sensor is not supported with your Band version. Microsoft Band 2 is required.\n");
					}
				} else {
					appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
				}
			} catch (BandException e) {
				String exceptionMessage="";
				switch (e.getErrorType()) {
					case UNSUPPORTED_SDK_VERSION_ERROR:
						exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
						break;
					case SERVICE_ERROR:
						exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
						break;
					default:
						exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
						break;
				}
				appendToUI(exceptionMessage);

			} catch (Exception e) {
				appendToUI(e.getMessage());
			}
			return null;
		}
	}

	private class AccelerometerSubscriptionTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
				if (getConnectedBandClient()) {
					appendToUI("Band is connected.\n");
					client.getSensorManager().registerAccelerometerEventListener(mAccelerometerEventListener, SampleRate.MS128);
				} else {
					appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
				}
			} catch (BandException e) {
				String exceptionMessage="";
				switch (e.getErrorType()) {
					case UNSUPPORTED_SDK_VERSION_ERROR:
						exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
						break;
					case SERVICE_ERROR:
						exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
						break;
					default:
						exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
						break;
				}
				appendToUI(exceptionMessage);

			} catch (Exception e) {
				appendToUI(e.getMessage());
			}
			return null;
		}
	}




	private boolean getConnectedBandClient() throws InterruptedException, BandException {
		if (client == null) {
			BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
			if (devices.length == 0) {
				appendToUI("Band isn't paired with your phone.\n");
				return false;
			}
			client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
		} else if (ConnectionState.CONNECTED == client.getConnectionState()) {
			return true;
		}
		
		appendToUI("Band is connecting...\n");
		return ConnectionState.CONNECTED == client.connect().await();
	}

	private class AsyncT extends AsyncTask<Void,Void,Void>{

		@Override
		protected Void doInBackground(Void... params) {


			try {

				URL url = new URL(url_link+":80"); //Enter URL here
				System.out.print(url_link);
				HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
				httpURLConnection.setDoOutput(true);
				httpURLConnection.setRequestMethod("POST"); // here you are telling that it is a POST request, which can be changed into "PUT", "GET", "DELETE" etc.
				httpURLConnection.setRequestProperty("Content-Type", "application/json"); // here you are setting the `Content-Type` for the data you are sending which is `application/json`
				httpURLConnection.connect();

				JSONObject jsonObject = new JSONObject();
				jsonObject.put("gsrValue", gsrValue);
                jsonObject.put("heartRate", heartRate);
                jsonObject.put("hearRateQuality",heartRateQuality);
				jsonObject.put("accelX",accelX);
				jsonObject.put("accelY",accelY);
				jsonObject.put("accelZ",accelZ);
				DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
				wr.writeBytes(jsonObject.toString());
				wr.flush();
				wr.close();

			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return null;
		}


	}
}
