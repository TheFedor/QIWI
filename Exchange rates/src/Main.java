import java.io.*;
import java.time.LocalDate;
import java.net.URL;
import java.net.HttpURLConnection;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Scanner;

public class Main {

    //адрес для получения xml с ценами и расшифровкой кода валют
    public static String currencyPriceURL = "https://www.cbr.ru/scripts/XML_daily.asp?date_req=";
    //формат вводимых данных
    public static final String DATA_INPUT_FORMAT = "currency_rates\\s\\s*--code=[A-Z]{3}\\s\\s*--date=\\d{4}-\\d{2}-\\d{2}";

    public static void main(String[] args){
        //получаем сообщение от пользователя в массив строк
        String inputMessage = readMessage("Введите данные: ");

        //вводем переменные начала и окончания подстроки. Они еще понадобятся
        int startIndex = 0;
        int endIndex = 0;

        //получаем дату в необходимом для завпроса формате
        startIndex = inputMessage.indexOf("--date=") + 7;
        String[] date = inputMessage.substring(startIndex).split("-");
        currencyPriceURL += date[2] + "/" + date[1] + "/" + date[0];

        //получаем строку со всеми данными по дате
        String allOnDateResults = sendRequest(currencyPriceURL, null, null);

        //получаем код валюты, введенный в консоль
        startIndex = inputMessage.indexOf("--code=") + 7;
        endIndex = startIndex + 3;
        String currencyCode = inputMessage.substring(startIndex, endIndex);

        //выделяем необходимую подстроку из строки результатов по всем валютам
        startIndex = allOnDateResults.indexOf(currencyCode);
        String oneCurrencyResult = allOnDateResults.substring(startIndex);
        endIndex = oneCurrencyResult.indexOf("</Value>");
        oneCurrencyResult = oneCurrencyResult.substring(0, endIndex);

        //начинаем формировать строку для вывода в консоль
        StringBuilder result = new StringBuilder();
        result.append(currencyCode).append(" (");
        //добавляем в строку для вывода перевод кода валюты
        startIndex = oneCurrencyResult.indexOf("<Name>") + 6;
        endIndex = oneCurrencyResult.indexOf("</Name>");
        result.append(oneCurrencyResult.substring(startIndex, endIndex)).append("): ");
        //добавляем в строку вывода цену на валюту
        startIndex = oneCurrencyResult.indexOf("<Value>") + 7;
        result.append(oneCurrencyResult.substring(startIndex));

        System.out.println(result);

    }

    //метод для получения данных от пользователя (консольный ввод)
    public static String readMessage(String message) {
        System.out.println(message);
        //будем использовать Scanner, так как ввод очень короткий
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String inputData = scanner.nextLine().trim();
            if (inputData.isEmpty())
                continue;

            //проверяем соответствуют ли введенные данные паттерну
            //если соответствуют, со вводом данных заканчиваем
            if (inputData.matches(DATA_INPUT_FORMAT))
            {
                inputData = inputData.replaceAll("\\s+", "");
                scanner.close();
                return inputData;
            }
            else
            {
                System.out.println("Invalid input data");
                return readMessage(message);
            }
        }
        System.out.println("input some data");
        return readMessage(message);
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
            System.out.println("sendRequest faild");
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