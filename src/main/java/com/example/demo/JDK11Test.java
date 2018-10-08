package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @author ： fjl
 * @date ： 2018/10/8/008 10:10
 */
public class JDK11Test {

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        //test1();
        //test2();
        //testgetSync();
        //testgetAsync();
        //testpostForm();
        //testpostJson();
        testConcurrentRequests();
    }

    public static void test1() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(5000))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://openjdk.java.net/"))
                .timeout(Duration.ofMillis(6000))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
    }

    public static void test2() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(5000))
                .authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("admin", "123".toCharArray());
                    }
                })
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/json/info"))
                .timeout(Duration.ofMillis(2000))
                .header("Cookie", "JSESSIONID=4f994730-32d7-4e22-a18b-25667ddeb636; userId=java11")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        System.out.println(response.statusCode());
    }

    /**
     * get 同步请求
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public static void testgetSync() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.baidu.com"))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
    }

    /**
     * 异步请求
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static void testgetAsync() throws ExecutionException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.baidu.com"))
                .build();
        CompletableFuture<Object> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
        System.out.println(future.get());
    }

    /**
     * POST表单
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public static void testpostForm() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/json/info"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("name1=1&name=2"))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.statusCode());
    }

    /**
     * POST JSON
     * @throws JsonProcessingException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static void testpostJson() throws JsonProcessingException, ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        User user = new User();
        user.setPassword("123");
        user.setUsername("lisi");
        String s = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(user);

        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/json/info1"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(s))
                .build();
        CompletableFuture<User> res = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    try {
                        return objectMapper.readValue(body, User.class);
                    } catch (IOException e) {
                        return new User();
                    }
                });
        System.out.println(res.get());
    }

    /**
     * 上传文件
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    public  void testUploadFile() throws IOException, InterruptedException, URISyntaxException {
        HttpClient client = HttpClient.newHttpClient();
        Path path = Path.of(getClass().getClassLoader().getResource("body.txt").toURI());
        File file = path.toFile();

        String multipartFormDataBoundary = "Java11HttpClientFormBoundary";
        org.apache.http.HttpEntity multipartEntity = MultipartEntityBuilder.create()
                .addPart("file", new FileBody(file, ContentType.DEFAULT_BINARY))
                //要设置，否则阻塞
                .setBoundary(multipartFormDataBoundary)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/file/upload"))
                .header("Content-Type", "multipart/form-data; boundary=" + multipartFormDataBoundary)
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> {
                    try {
                        return multipartEntity.getContent();
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());
    }

    /**
     * 下载文件
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static void testAsyncDownload() throws ExecutionException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/file/download"))
                .build();

        CompletableFuture<Path> result = client.sendAsync(request, HttpResponse.BodyHandlers.ofFile(Paths.get("/tmp/body.txt")))
                .thenApply(HttpResponse::body);
        System.out.println(result.get());
    }


    /**
     * 并发请求
     */
    public static void testConcurrentRequests(){
        HttpClient client = HttpClient.newHttpClient();
        List<String> urls = List.of("http://www.baidu.com","http://www.alibaba.com/","http://www.tencent.com");
        List<HttpRequest> requests = urls.stream()
                .map(url -> HttpRequest.newBuilder(URI.create(url)))
                .map(reqBuilder -> reqBuilder.build())
                .collect(Collectors.toList());

        List<CompletableFuture<HttpResponse<String>>> futures = requests.stream()
                .map(request -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .collect(Collectors.toList());
        futures.stream()
                .forEach(e -> e.whenComplete((resp,err) -> {
                    if(err != null){
                        err.printStackTrace();
                    }else{
                        System.out.println(resp.body());
                        System.out.println(resp.statusCode());
                    }
                }));
        CompletableFuture.allOf(futures
                .toArray(CompletableFuture<?>[]::new))
                .join();
    }


}
