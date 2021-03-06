package MyHTTPServer;


import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static MyHTTPServer.SocketUtils.receiveMessage;
import static MyHTTPServer.SocketUtils.sendMessage;


class MyHTTPThread extends Thread {
    private final Socket requestSocket;
    private final String ipControl;
    private final int pControl;

    MyHTTPThread(Socket requestSocket, String ip, int puerto) {
        this.requestSocket = requestSocket;
        this.ipControl = ip;
        this.pControl = puerto;
    }

    @Override
    public void run() {
        try {
            String request = receiveMessage(requestSocket);
            String[] requestParts = request.split(" ");
            if (requestParts[0].equals("GET")) {
                String URL = requestParts[1];
                if (URL.equals("/")) URL += ("index.html");
                String[] routeParts = URL.split("/");
                if (routeParts[1].equals("controladorSD"))
                    serveDynamicRequest(routeParts.length == 3 ? routeParts[2] : "");
                else serveStaticRequest(routeParts[1]);
            } else {
                System.err.println("ERROR 405 for client " + requestSocket.getRemoteSocketAddress() + ". Requested method: " + requestParts[0]);
                sendHTTPResponse(MyHTTPStatusCode.METHOD_NOT_ALLOWED.getHTMLError(), MyHTTPStatusCode.METHOD_NOT_ALLOWED);
            }
            requestSocket.close();
        } catch (IOException e) {
            System.err.println("ERROR: Unable to open inputStream from socket");
            e.printStackTrace();
        }
    }

  
    private void serveDynamicRequest(String query) throws IOException {
        try {
            Socket s = new Socket(ipControl, pControl);
            sendMessage(s, query + "\n\r");
            String response = receiveMessage(s);
            s.close();
            if (!response.replaceAll("\\n","").equals("null")) sendHTTPResponse(response, MyHTTPStatusCode.OK);
            else throw new IOException();
        } catch (IOException e) {
            sendHTTPResponse(MyHTTPStatusCode.CONFLICT.getHTMLError(), MyHTTPStatusCode.CONFLICT);
        }
    }


    private void serveStaticRequest(String path) throws IOException {
        try {
            sendHTTPResponse(HTMLEntityEncode(new String(Files.readAllBytes(Paths.get(path)))) + "\n", MyHTTPStatusCode.OK);
        } catch (IOException e) {
            try {
                sendHTTPResponse(MyHTTPStatusCode.NOT_FOUND.getHTMLError(), MyHTTPStatusCode.NOT_FOUND);
            } catch (IOException e1) { e1.printStackTrace(); }
            System.err.println("Client " + requestSocket.getRemoteSocketAddress() + " got ERROR 404 for file " + path);
        }
    }


    private void sendHTTPResponse(String fileContent, MyHTTPStatusCode statusCode) throws IOException {
        String response = "HTTP/1.1 " + statusCode.getCode() + " " + statusCode.getDescription() + "\n" +
                "Connection: close\n" +
                "Content-Length: " + fileContent.length() + "\n" +
                "Content-Type: text/html\n" +
                "Server: practicas-sd\n" +
                "\n" + //Headers end with an empty line
                fileContent;
        sendMessage(requestSocket, response);
    }

 
    static String HTMLEntityEncode(String s) {
        StringBuilder builder = new StringBuilder ();
        for (char c : s.toCharArray()) builder.append((int)c < 128 ? c : "&#" + (int) c + ";");
        return builder.toString();
    }
}
