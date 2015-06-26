package com.myappscoauthorization;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.os.AsyncTask;

/**
 * Custom exception handler to maintain error logs.
 * @author Amol Wadekar
 */
public class ExceptionHandler implements UncaughtExceptionHandler{

	UncaughtExceptionHandler uncaughtExceptionHandler;
	String URL;
	public ExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler, String URL){
		this.uncaughtExceptionHandler=uncaughtExceptionHandler;
		this.URL=URL;
	}
	
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		
		StringWriter localStringWriter = new StringWriter();
		Calendar cal=Calendar.getInstance();
		localStringWriter.append("****************** ");
		localStringWriter.append(cal.get(Calendar.DATE)+"/"+cal.get(Calendar.MONTH)+"/"+cal.get(Calendar.YEAR)+" "+cal.get(Calendar.HOUR)+":"+cal.get(Calendar.MINUTE)+":"+cal.get(Calendar.SECOND));
		localStringWriter.append("  ****************** \n");
		PrintWriter localPrintWriter = new PrintWriter(localStringWriter);
	    ex.printStackTrace(localPrintWriter);
		
	    System.out.println(localStringWriter.toString());
	    
	    new  Asyn_errorlog().execute(localStringWriter);
	    
		this.uncaughtExceptionHandler.uncaughtException(thread, ex);
	}
	
	/**
	 * Asyn Task to print log
	 * @author Amol Wadekar
	 *
	 */
	class Asyn_errorlog extends AsyncTask<StringWriter, String, String>{
		StringWriter localStringWriter;
		@Override
		protected String doInBackground(StringWriter... params) {
			localStringWriter=params[0];
//			sendExceptionToLogger(localStringWriter);
			return null;
		}
		
/*		private void sendExceptionToLogger(StringWriter localStringWriter){
			//http://192.168.1.180/MyAppsCo/demo/version1/webservices/controller/clientController.php?action=AppCrashReport&report=sadfsdfsfsdfsdf
			try{
				
				System.out.println("In AsynTask "+localStringWriter.toString());
				
				String NameSpace="urn:myappsco";
				
				String url=URL;//"http://webservices.myappsco.com/controller/serverController.php";
				System.out.println("Creah Report URL "+URL);
				String methodName="AppCrashReport";
				String SOAP_ACTION=NameSpace+"#"+methodName;

				SoapObject soapObject=new SoapObject(NameSpace, methodName);
				SoapSerializationEnvelope envelope=new  SoapSerializationEnvelope(SoapSerializationEnvelope.VER11);
				soapObject.addProperty("report", localStringWriter.toString());
				envelope.setOutputSoapObject(soapObject);
				
				List<HeaderProperty> headers=new ArrayList<HeaderProperty>();
				HeaderProperty headerProperty=new HeaderProperty("Accept-Encoding", "none");
				headers.add(headerProperty);
				
				String response=null;
				try{
					HttpTransportSE httpTransportSE=new HttpTransportSE(url);
					httpTransportSE.call(SOAP_ACTION, envelope,headers);
					response=(String)envelope.getResponse();
					System.out.println("AppCrashReport JSON Response "+response);
					
				}catch(Exception e){
					e.printStackTrace();
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}*/
		
	}
	
}
