package com.twilio;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.twilio.sdk.verbs.TwiMLResponse;
import com.twilio.sdk.verbs.TwiMLException;
import com.twilio.sdk.verbs.Message;

public class TwilioServlet extends HttpServlet {

	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// Requests come in the form: Request: 320, 341, etc.
		String bodyOfRequest = request.getParameter("Body");
		String busesRequestedSplitByCommas = bodyOfRequest.split("Request:")[1];
		String[] busesRequested = busesRequestedSplitByCommas.split(",");
		//String myResponse = "Hello, " + request.getParameter("From");
		// TODO: Work without the assumption that there will always be at least
		// one bus requested.
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
		}
	}
}
