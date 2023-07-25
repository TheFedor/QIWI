import java.io.*;
import java.time.LocalDate;
import java.net.URL;
import java.net.HttpURLConnection;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Scanner;

public class Main {

    //адреса для получчения цены всех валют на определенную дату и кода всех валют с расшифровкой
    public static String priceURL = "https://www.cbr.ru/scripts/XML_daily.asp?date_req=";

    public static void main(String[] args) throws IOException {
        //получаем сообщение от пользователя в массив строк
        String[] inputMessage = readMessage("Введите данные в формате: curency_rate --code=CODE --date=YYYY-MM-DD").trim().split(" ");
        //получаем точный запрос для получения всех валют в дату
        String[] date = inputMessage[2].replace("--date=", "").split("-");
        priceURL = priceURL + date[2] + "/" + date[1] + "/" + date[0];

        //получаем все курсы по дате
        String resultAllPrices = Main.sendRequest(priceURL, null, null);

        //получаем из всех крсов по дате нужный для вывода результат
        String code = inputMessage[1].replace("--code=", "");
        int indexStart = resultAllPrices.indexOf(code);
        String subString = resultAllPrices.substring(indexStart);
        int indexEnd = subString.indexOf("</Value>");
        subString = subString.substring(0, indexEnd);
        System.out.println(subString);
        //теперь отсюда запоминаем название валюты и ее цену
        StringBuilder result = new StringBuilder();
        result.append(code).append(" (");
        //получаем название валюты
        indexStart = subString.indexOf("<Name>") + 6;
        indexEnd = subString.indexOf("</Name>");
        result.append(subString.substring(indexStart, indexEnd)).append("): ");
        //получаем цену валюты
        indexStart = subString.indexOf("<Value>") + 7;
        result.append(subString.substring(indexStart));
        System.out.println(result);


        //получаем данные от пользователя (консольный ввод)
        //String data = readMessage("Введите данные в формате: curency_rate --code=CODE --date=YYYY-MM-DD");

    }

    //метод для получения данных от пользователя (консольный ввод)
    public static String readMessage(String message) throws IOException {
        System.out.println(message);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        return reader.readLine();
    }

    /**
     * @param url       - required
     * @param headers   - nullable
     * @param request   - nullable
     */
    public static String sendRequest(String url, Map<String, String> headers, String request) {
        String result = null;
        HttpURLConnection urlConnection = null;
        try {
            URL requestUrl = new URL(url);
            urlConnection = (HttpURLConnection) requestUrl.openConnection();
            urlConnection.setReadTimeout(20000);
            urlConnection.setConnectTimeout(20000);
            urlConnection.setRequestMethod("GET");

            if (request != null) {
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                DataOutputStream outputStream = new DataOutputStream(urlConnection.getOutputStream());
                outputStream.writeBytes(request);
                outputStream.flush();
                outputStream.close();
            }

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    urlConnection.addRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            int status = urlConnection.getResponseCode();
            System.out.println("status code:" + status);

            if (status == HttpURLConnection.HTTP_OK) {
                result = getStringFromStream(urlConnection.getInputStream());
            }
        }
        catch (Exception e){
            System.out.println("FAILED");
        }
        finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return result;
    }

    private static String getStringFromStream(InputStream inputStream) throws IOException {
        final int BUFFER_SIZE = 4096;
        ByteArrayOutputStream resultStream = new ByteArrayOutputStream(BUFFER_SIZE);
        byte[] buffer = new byte[BUFFER_SIZE];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            resultStream.write(buffer, 0, length);
        }
        return resultStream.toString("UTF-8");
    }
}