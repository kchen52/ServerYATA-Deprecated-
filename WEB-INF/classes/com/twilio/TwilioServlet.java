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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwilioServlet extends HttpServlet {
    public static String ACCOUNT_SID = "NULL";
    public static String AUTH_TOKEN = "NULL";
    public static String TWILIO_NUMBER = "NULL";
    public static String TRANSLINK_API = "NULL";

    private int MAX_OUTGOING_MESSAGE_LENGTH = 1550;

    // TODO: Read from a specific folder
    public static final String CREDENTIALS_FILENAME = "/home/ubuntu/credentials";

    static {
        initializeCredentials(CREDENTIALS_FILENAME);
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Requests come in the form: Request: 320, 341, etc.
        String requestPhoneNumber = request.getParameter("From");
        String bodyOfRequest = request.getParameter("Body");

        // TODO: Use regex to check that the body follows the format
        //Pattern properForm = Pattern.compile("Request: (\\d,)*
        // if (bodyOfRequest.....)
        String busesRequestedSplitByCommas = bodyOfRequest.split("Request: ")[1];
        String[] busesRequested = busesRequestedSplitByCommas.split(",");

        // If no buses are requested...
        if (busesRequested.length == 0) {
            // Debug message for now
            sendSMS(requestPhoneNumber, "At least one bus should be requested lol");
        }

        for (int i = 0; i < busesRequested.length; i++) {
            String currentBus = busesRequested[i];
            String busRequestURL = "http://api.translink.ca/rttiapi/v1/buses?apikey=" + 
                TRANSLINK_API + "&routeNo=" + currentBus;
            String busInformation = getHTML(busRequestURL);

            ArrayList<Bus> buses = new ArrayList<Bus>();
            // Separate buses 
            Pattern busPattern = Pattern.compile("<Bus>(.*?)</Bus>");
            Matcher matcher = busPattern.matcher(busInformation);
            while (matcher.find()) {
                Bus bus = new Bus();
                bus.init(matcher.group());
                buses.add(bus);
            }

            // TODO: Send by bus type (e.g., Langley ctr vs surrey central)
            buses = (ArrayList<Bus>)mergeSort(buses);
            StringBuilder builder = new StringBuilder();

            String lastDestination = "NULL";
            for (Bus bus : buses) {
                String currentBusDestination = bus.getDestination();
                if (!currentBusDestination.equals(lastDestination)) {
                    builder.append(currentBusDestination);
                    builder.append("-");
                } 
                builder.append(bus.getVehicleNumber());
                builder.append(":");
                builder.append(bus.getLongitude());
                builder.append(",");
                builder.append(bus.getLongitude());
                builder.append("|");
                lastDestination = currentBusDestination;
            }

            //currentBus.init(busRequestURL);
            //sendSMS(requestPhoneNumber, "Initialized " + count + " buses.");
            sendSMS(requestPhoneNumber, builder.toString());
            response.setContentType("application/xml");
            response.getWriter().print("<Nothing></Nothing>");
        }
    }

    private void sendSMS(String recipient, String messageToSend) {
        // NOTE: There is a 1600 character limit imposed by Twilio, so split messages
        // accordingly
        ArrayList<String> splitMessages = new ArrayList<String>();
        int lowerIndex = Math.min(MAX_OUTGOING_MESSAGE_LENGTH, messageToSend.length());
        while (lowerIndex >= MAX_OUTGOING_MESSAGE_LENGTH) {
            splitMessages.add(messageToSend.substring(0, MAX_OUTGOING_MESSAGE_LENGTH));
            messageToSend = messageToSend.substring(MAX_OUTGOING_MESSAGE_LENGTH, messageToSend.length());
            lowerIndex = Math.min(MAX_OUTGOING_MESSAGE_LENGTH, messageToSend.length());
        }
        // However, if the message was less than 1600 characters, we can just send it as is
        if (splitMessages.size() == 0) {
            splitMessages.add(messageToSend);
        }

        try {
            TwilioRestClient client = new TwilioRestClient(ACCOUNT_SID, AUTH_TOKEN);
            Account account = client.getAccount();
            MessageFactory messageFactory = account.getMessageFactory();
            for (String subMessage : splitMessages) {
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("To", recipient));
                params.add(new BasicNameValuePair("From", TWILIO_NUMBER));
                params.add(new BasicNameValuePair("Body", subMessage));
                Message sms = messageFactory.create(params);
            }
        } catch (TwilioRestException e) {
            e.printStackTrace();
        }

        // Because of the way Twilio is configured to interact with this, we need to give it a command
        //TwiMLResponse twiml = new twiMLResponse()
    }

    // Taken from http://stackoverflow.com/questions/1485708/how-do-i-do-a-http-get-in-java
    private String getHTML(String urlToRead) {
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL(urlToRead);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(); conn.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                result.append(currentLine);
            }
            reader.close();
        } catch (IOException e) {
            // No body, return an appropriate msg
            return "NO RESULTS AVAILABLE";
        }
        return result.toString();
    }

    private static void initializeCredentials(String fileName) {
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

    // Run of the mill mergesort. In our case, we're sorting by bus destination
    private List<Bus> mergeSort(List<Bus> arrayToSort) {
        if (arrayToSort.size() <= 1) {
            return arrayToSort;
        }
        // Note that startIndex is inclusive
        // while endIndex is exclusive. That's why the following looks like it would have duplicate
        // values, but actually sorts properly
        List<Bus> firstHalf = mergeSort(arrayToSort.subList(0, arrayToSort.size()/2));
        List<Bus> secondHalf = mergeSort(arrayToSort.subList(arrayToSort.size()/2, arrayToSort.size()));
        List<Bus> mergedResult = merge(firstHalf, secondHalf);
        return mergedResult;
    }

    private List<Bus> merge(List<Bus> firstHalf, List<Bus> secondHalf) {
        List<Bus> merge = new ArrayList<Bus>();
        int firstHalfIndex = 0;
        int secondHalfIndex = 0;
        while (firstHalfIndex < firstHalf.size() && secondHalfIndex < secondHalf.size()) {
            String firstHalfValue = firstHalf.get(firstHalfIndex).getDestination();
            String secondHalfValue = secondHalf.get(secondHalfIndex).getDestination();

            if (firstHalfValue.compareTo(secondHalfValue) <= 0) {
                merge.add(firstHalf.get(firstHalfIndex));
                firstHalfIndex++;
            } else if (firstHalfValue.compareTo(secondHalfValue) > 0) {
                merge.add(secondHalf.get(secondHalfIndex));
                secondHalfIndex++;
            }
        }

        while (firstHalfIndex < firstHalf.size()) {
            merge.add(firstHalf.get(firstHalfIndex));
            firstHalfIndex++;
        }

        while (secondHalfIndex < secondHalf.size()) {
            merge.add(secondHalf.get(secondHalfIndex));
            secondHalfIndex++;
        }
        return merge;
    }


}
