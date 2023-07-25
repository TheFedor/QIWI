import java.io.*;
import java.nio.file.Paths;
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

    public static void main(String[] args) throws IOException {

        //создаем заполняем файл, который будет выступать источником запросов во время выполнения программы, из файла первоисточника
        try {
            File file = new File("tests.txt");
            file.createNewFile();
        }
        catch (IOException e){
            System.err.println("Error with file creating: " + e.getMessage());
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader("testsOriginal.txt"));
            BufferedWriter writer = new BufferedWriter(new FileWriter("tests.txt"));
            //переписываем данные из файла-первоисточника
            String line = "";
            while ((line = reader.readLine()) != null)
            {
                writer.write(line);
                writer.newLine();
            }

            reader.close();
            writer.close();

        }
        catch (IOException e){
            System.err.println("Error with file writing: " + e.getMessage());
        }

        //получаем сообщение от пользователя в массив строк
        System.out.println(readMessage("Введите данные: "));
        System.out.println("\nend");

        //удаляем файл, который выступал источником в программе
        File file = new File("tests.txt");
        file.delete();

    }

    //метод для удаления первой строки из файла
    public static void removeFirstRowFromFile(String filename) {
        //создаем полную копию исходного файла
        String copyFilename = filename + "-copy";
        try {
            File fileCopy = new File(copyFilename);
            fileCopy.createNewFile();
        }
        catch (IOException e){
            System.err.println("Error with file creating: " + e.getMessage());
        }

        //переносим все строки, кроме первой, в файл-копию
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            BufferedWriter writer = new BufferedWriter(new FileWriter(copyFilename));
            //пропускаем первую строку файла-источника
            reader.readLine();
            //переписываем оставшиеся данные из файла-источника
            String line = "";
            while ((line = reader.readLine()) != null)
            {
                writer.write(line);
                writer.newLine();
            }
            reader.close();
            writer.close();

        }
        catch (IOException e){
            System.err.println("Error with file writing: " + e.getMessage());
        }

        //удаляем файл-источник и создаем новый пустой файл, который впоследствии будет новым источником
        File fileSource = new File(filename);
        fileSource.delete();
        try {
            fileSource.createNewFile();
        }
        catch (IOException e){
            System.err.println("Error with file creating: " + e.getMessage());
        }

        //перезаписываем все строки из файла-копии в новый файл-источник
        try {
            BufferedReader reader = new BufferedReader(new FileReader(copyFilename));
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

            //переписываем данные из файла-копии
            String line = "";
            while ((line = reader.readLine()) != null)
            {
                writer.write(line);
                writer.newLine();
            }
            reader.close();
            writer.close();

        }
        catch (IOException e){
            System.err.println("Error with file writing: " + e.getMessage());
        }

        //удаляем файл-копию
        File fileCopy = new File(copyFilename);
        fileCopy.delete();

    }

    //метод для получения данных от пользователя (консольный ввод)
    public static String readMessage(String message) throws IOException {
        System.out.println(message);
        //будем использовать Scanner, так как ввод очень короткий
        Scanner scanner = new Scanner(Paths.get("tests.txt"));
        if (scanner.hasNext()) {
            String inputData = scanner.nextLine().trim();

            //проверяем соответствуют ли введенные данные паттерну
            //если соответствуют, со вводом данных заканчиваем
            if (inputData.matches(DATA_INPUT_FORMAT))
            {
                inputData = inputData.replaceAll("\\s+", "");
                resultOutput(inputData);
                //удаляем прочитанную строку из файла. Если мы здесь, то она гарантированно есть
                removeFirstRowFromFile("tests.txt");
                scanner.close();
                return readMessage(message);
            }
            else
            {
                System.out.println("Invalid input data");
                return readMessage(message);
            }
        }
        //здесь теперь не вызываем повторно метод, а завершаем работу, так как данных больше нет
        return "dataset is empty";
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

    //метод для получения и вывода результата
    public static void resultOutput(String inputMessage)
    {
        System.out.println("inputMessage: " + inputMessage);
        //вводем переменные начала и окончания подстроки. Они еще понадобятся
        int startIndex = 0;
        int endIndex = 0;

        //получаем дату в необходимом для завпроса формате
        startIndex = inputMessage.indexOf("--date=") + 7;
        String[] date = inputMessage.substring(startIndex).split("-");
        //меняем данные даты на последний существующий день, если они больше
        if (Integer.valueOf(date[0]) > 2023)
        {
            date[0] = "2023";
            if (Integer.valueOf(date[1]) > 7)
            {
                date[1] = "07";
                if(Integer.valueOf(date[2]) > 25)
                {
                    date[2] = "25";
                }
            }
            else if (Integer.valueOf(date[1]) == 7)
            {
                if(Integer.valueOf(date[2]) > 25)
                {
                    date[2] = "25";
                }
            }
        }
        else if (Integer.valueOf(date[0]) == 2023)
        {
            if (Integer.valueOf(date[1]) > 7)
            {
                date[1] = "07";
                if(Integer.valueOf(date[2]) > 25)
                {
                    date[2] = "25";
                }
            }
            else if (Integer.valueOf(date[1]) == 7)
            {
                if(Integer.valueOf(date[2]) > 25)
                {
                    date[2] = "25";
                }
            }
        }

        //получаем полноценный запрос на данные по дате
        String localPriceURL = currencyPriceURL + date[2] + "/" + date[1] + "/" + date[0];

        //получаем строку со всеми данными по дате
        String allOnDateResults = sendRequest(localPriceURL, null, null);

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