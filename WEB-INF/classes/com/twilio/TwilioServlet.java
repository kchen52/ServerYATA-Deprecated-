package com.twilio;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.HashMap;

import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Message;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.net.*;

public class TwilioServlet extends HttpServlet {
	public String ACCOUNT_SID = "NULL";
	public String AUTH_TOKEN = "NULL";
	public String TWILIO_NUMBER = "NULL";
	public String TRANSLINK_API = "NULL";

	// TODO: Read from a specific folder
	public static final String CREDENTIALS_FILENAME = "/home/ubuntu/credentials";

	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO: Make it so credentials are only read once, upon startup... we don't need to keep
		// rereading them.
		// Read in credentials from credentials file
		initializeCredentials(CREDENTIALS_FILENAME);

		// Requests come in the form: Request: 320, 341, etc.
		// TODO: Sometimes texts may come in blank, might want to take care of that.
		String requestPhoneNumber = request.getParameter("From");
		String bodyOfRequest = request.getParameter("Body");

		// TODO: Use regex to check that the body follows the format
		// if (bodyOfRequest.....)
		String busesRequestedSplitByCommas = bodyOfRequest.split("Request: ")[1];
		String[] busesRequested = busesRequestedSplitByCommas.split(",");
		// TODO: Work without the assumption that there will always be at least
		// one bus requested.
		if (busesRequested.length == 0) {
			// Debug message for now
			sendSMS(requestPhoneNumber, "At least one bus should be requested lol");
		}

		for (int i = 0; i < busesRequested.length; i++) {
			String currentBus = busesRequested[i];
			String busRequestURL = "http://api.translink.ca/rttiapi/v1/buses?apikey=" + 
				TRANSLINK_API + "&routeNo=" + currentBus;
			sendSMS(requestPhoneNumber, getHTML(busRequestURL));
		}
	}

	private void sendSMS(String recipient, String messageToSend) {
		// TODO: Handle longer messages
		// NOTE: There is a 1600 character limit imposed by Twilio, so split messages
		// accordingly
		try {
			TwilioRestClient client = new TwilioRestClient(ACCOUNT_SID, AUTH_TOKEN);
			Account account = client.getAccount();
			MessageFactory messageFactory = account.getMessageFactory();
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("To", recipient));
			params.add(new BasicNameValuePair("From", TWILIO_NUMBER));
			params.add(new BasicNameValuePair("Body", messageToSend));
			Message sms = messageFactory.create(params);
		} catch (TwilioRestException e) {
			e.printStackTrace();
		}
	}

	// Taken from http://stackoverflow.com/questions/1485708/how-do-i-do-a-http-get-in-java
	private String getHTML(String urlToRead) {
		StringBuilder result = new StringBuilder();
		try {
			URL url = new URL(urlToRead);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String currentLine;
			while ((currentLine = reader.readLine()) != null) {
				result.append(currentLine);
			}
			reader.close();
		} catch (IOException e) {
			// No body, return an appropriate msg
			return "NO RESULTS";
		}
		return result.toString();
	}

	private void initializeCredentials(String fileName) {
		// We know there will only be 3 lines, so just do this sloppy for now
		// TODO: Make this unsloppy
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
			String firstLine = bufferedReader.readLine();
			String secondLine = bufferedReader.readLine();
			String thirdLine = bufferedReader.readLine();
			String fourthLine = bufferedReader.readLine();

			ACCOUNT_SID = firstLine.split("ACCOUNT_SID = ")[1];
			AUTH_TOKEN = secondLine.split("AUTH_TOKEN = ")[1];
			TWILIO_NUMBER = thirdLine.split("TWILIO_NUMBER = ")[1];
			TRANSLINK_API = fourthLine.split("TRANSLINK_API = ")[1];

			bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
