package com.twilio;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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

import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;

public class TwilioServlet extends HttpServlet {
	public String ACCOUNT_SID = "NULL";
	public String AUTH_TOKEN = "NULL";
	public String TWILIO_NUMBER = "NULL";
	// TODO: Read from a specific folder
	public static final String CREDENTIALS_FILENAME = "/home/ubuntu/credentials";

	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO: Make it so credentials are only read once, upon startup... we don't need to keep
		// rereading them.
		// Read in credentials from credentials file
		initializeCredentials(CREDENTIALS_FILENAME);

		sendSMS("16044403468", "Test message");

		/*// Requests come in the form: Request: 320, 341, etc.
		// TODO: Sometimes texts may come in blank, might want to take care of that.
		String bodyOfRequest = request.getParameter("Body");
		String busesRequestedSplitByCommas = bodyOfRequest.split("Request:")[1];
		String[] busesRequested = busesRequestedSplitByCommas.split(",");
		// TODO: Work without the assumption that there will always be at least
		// one bus requested.
		if (busesRequested.length == 0) {
		// Don't return anything
		// This following line is purely for debug purposes
		TwiMLResponse twiml = new TwiMLResponse();
		Message message = new Message ("Returning nothing");
		try {
		twiml.append(message);
		} catch (TwiMLException e) {
		e.printStackTrace();
		}
		response.setContentType("application/xml");
		response.getWriter().print(twiml.toXML());
		return;
		} 
		for (int i = 0; i < busesRequested.length; i++) {
		String stuffToReturn = busesRequested[i];
		TwiMLResponse twiml = new TwiMLResponse();
		Message message = new Message(stuffToReturn);
		try {
		twiml.append(message);
		} catch (TwiMLException e) {
		e.printStackTrace();
		}
		response.setContentType("application/xml");
		response.getWriter().print(twiml.toXML());
		}*/
	}

	private void sendSMS(String recipient, String messageToSend) {
		// TODO: Handle longer messages
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

	private void initializeCredentials(String fileName) {
		// We know there will only be 3 lines, so just do this sloppy for now
		// TODO: Make this unsloppy
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
			String firstLine = bufferedReader.readLine();
			String secondLine = bufferedReader.readLine();
			String thirdLine = bufferedReader.readLine();

			ACCOUNT_SID = firstLine.split("ACCOUNT_SID = ")[1];
			AUTH_TOKEN = secondLine.split("AUTH_TOKEN = ")[1];
			TWILIO_NUMBER = thirdLine.split("TWILIO_NUMBER = ")[1];

			bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
