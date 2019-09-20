import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Die Klasse WSGUploader demonstriert wie in Java AM-XML Requests via Web Service zum Web Service Gateway der
 * Deutschen Post übertragen werden können und wie die Antwort des Web Service "gelesen" werden kann.
 *
 * (C) Deutsche Post AG
 *
 */
public class WSGUploader implements Uploader {

    // Der docBuilder wird später zum Einlesen bzw. zum Parsen des AM-XML Stroms verwendet
    private DocumentBuilder docBuilder;


    // Die hier definierte konstante Zeichenkette entspricht dem Teil des SOAP-Envelopes, der vor dem
    // AM-XML Payload in der SOAP-Nachricht steht. (Vergleichen Sie dazu auch das mitgelieferte SOAP-Envelope Beispiel.)
    // Die Zeichen "XXX" müssen durch den korrekten Username bzw. Passwort für das jeweilige System, degen das der
    // Request laufen soll, ersetzt werden. Die richtigen Angaben dafür erhalten Sie von Ihrem Ansprechpartner beim IT-CSP.
    //
    private static final String WSG_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "  xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/03/addressing\"\n" +
            "  xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"\n" +
            "  xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n" +
            "  <soap:Header>\n" +
            "    <wsse:Security soap:mustUnderstand=\"1\">\n" +
            "      <wsse:UsernameToken\n" +
            "        xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"\n" +
            "        wsu:Id=\"SecurityToken-115b71ae-7ae2-47c6-a1cd-87c540312330\">\n" +
            "        <wsse:Username>XXX</wsse:Username>\n" +
            "        <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\" >xxx</wsse:Password>\n" +
            "      </wsse:UsernameToken>\n" +
            "    </wsse:Security>\n" +
            "  </soap:Header>\n" +
            "  <soap:Body>";


    // Die hier definierte konstante Zeichenkette entspricht dem Teil des SOAP-Envelopes, der nach dem
    // AM-XML Payload in der SOAP-Nachricht steht. WSG_Header und WSG_Footer schliessen den AM-XML Payload der
    // SOAP-Nachricht ein. (Vergleichen Sie dazu auch das mitgelieferte SOAP-Envelope Beispiel.)
    private static final String WSG_FOOTER = "</soap:Body>\n" +
            "</soap:Envelope>";


    /*
     * Der Konstruktor der Klasse, der für die Member-Variable docBuilder das entsprechende
     * Java-Objekt erzeugt und zuweist.
     */
    public WSGUploader() throws Exception {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        docBuilder = builderFactory.newDocumentBuilder();
    }



    /*
     * Die upload-Methode versendet den beim Aufruf der Methode mitgelieferten AM-XML Request zum Web Service und
     * ruft die ebenfalls beim Request mitgelieferte Service-Operation auf.
     * 
     * @param  xmlContent        Der zu versendende AM-XML Request (Payload der SOAP-Nachricht) als String.
     *
     * @param  serviceOperation  Der Name der Service-Operation des Web Services, die aufgerufen werden soll.
     *                           Mögliche Werte sind:
                                 - "createOrder"
                                 - "changeOrder"
                                 - "cancelOrder"
                                 - "seekOrder"
                                 - "getOrder"
     *
     * @param  serviceVersion    Die Version des (certification)-OrderManagement Services, die aufgerufen werden soll.
     *                           Derzeit ist hier nur der Wert "2.0" erlaubt.
     *
     * @param  useCertSystem     Bei true wird das AM-Zertifizierungssystem verwendet, sonst das AM-Produktivsystem
     *
     */
    public void upload(String xmlContent, String serviceOperation, String serviceVersion, boolean useCertSystem) throws Exception {

        // Zum Aufnehmen der Response des Web Service Aufrufes
        String xmlResponse;

	// Der übergebene AM-XML Request wird vor dem versenden zum Web Service minimal bearbeitet.
        String soapBody = xmlContent.replaceFirst("<\\?xml[^>]*\\?>", "");

        // Die im Entwicklerhandbuch angegebene Basis-URL des Web Service Gateways der Deutschen Post
        URL wsgUrl = new URL("https://sop-ws.deutschepost.de/sbb/services/Invoke");

        // Erzeugung des wesentlichen Java-Objektes, nämlich einer HTTP-Verbindung, die zum Aufbau 
        // und Handling einer Verbindung zum Web Service Gateway benötigt wird.
        HttpURLConnection httpConnection = (HttpURLConnection)wsgUrl.openConnection();

        // Es soll die Methode "POST" beim Versenden des Requests verwenden werden.
        httpConnection.setRequestMethod("POST");

        // Es wird XML-Text in der UTF-8 Codierung versendet.
        httpConnection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");

        // Es soll die im Methodenaufruf übergebene Service-operation als "SOAP-Action" aufgerufen werden.
        // Je nach Wert des Flags "useCertSystem" wird der Request entweder zur AM Zertifizierungsumgebung oder 
        // zum AM-Produktivsystem übermittelt.
        if (useCertSystem == true) {
          // Ziel des Requests ist die AM-Zertifizierungsumgebung, d.h. der CertificationOrderManagement Service in Version 4.4
          httpConnection.addRequestProperty("SOAPAction", "OrderManagement/CertificationOrderManagement/" + serviceVersion + "#" + serviceOperation);
        }
        else
        {
          // Ziel des Requests ist die AM-Produktivumgebung, d.h. der OrderManagement Service in Version 4.4
          httpConnection.addRequestProperty("SOAPAction", "OrderManagement/OrderManagement/" + serviceVersion + "#" + serviceOperation);
        }

        // Der Output der Operation soll erzeugt und zurückgegeben werden.
        httpConnection.setDoOutput(true);

        // Aufbau der Verbindung zum Web Service
        httpConnection.connect();

        // Der OutputStreamWriter der Verbindung wird der Variablen outStream zugewiesen.
        // Er wird hier direkt zum Versenden des kompletten SOAP-Requests über die HTTP-Verbindung verwendet.
        OutputStreamWriter outStream = new OutputStreamWriter(httpConnection.getOutputStream(), "UTF-8");
        outStream.write(WSG_HEADER);
        outStream.write('\n');
        outStream.write(soapBody);
        outStream.write('\n');
        outStream.write(WSG_FOOTER);
        outStream.write('\n');
        outStream.flush();

        // Abfrage des Fehlercodes nach Versenden des SOAP-Requests über die HTTP-Verbindung
        int responseCode = httpConnection.getResponseCode();

        if (responseCode == 200) {
            // Falls keine Fehler aufgetreten sind, wird die Response des Web Service Aufrufes zeichenweise
            // aus dem "InputStream" der HTTP-Verbindung gelesen und danach der Zeichenkette xmlResponse zugewiesen
            //
            InputStream responseStream = httpConnection.getInputStream();

            // Korrekte Interpretation der UTF-8 Kodierung wird mit Hilfe eines BufferedReader und InputStreamReader gewährleistet
            final BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream, "UTF-8"));

            // Antwort der Schnittstelle in String ausgeben
            StringBuffer result = new StringBuffer();
            String line;

            while((line = reader.readLine()) != null) {
                result.append(line + "\n");
            }
            responseStream.close();
            xmlResponse = result.toString();

        } else {
            // Falls Fehler aufgetreten sind, wird eine runtimeException geworfen.
            throw new RuntimeException("Gateway returned response code " + responseCode);
        }

	/*
         * Und hier kann letztlich die Verarbeitung der AM-XML Response (xmlResponse) erfolgen.
         */
    }
}
