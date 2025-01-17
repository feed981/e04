package com.feddoubt.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feddoubt.model.YT1.pojos.VideoDetails;
import com.feddoubt.model.YT1.pojos.YtdlpDumpjson;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ContainerTest {
    private static final String vagrantfile = "D:\\VirtualBox VMs\\vagrant-ubuntu";
    public static void main(String[] args) throws IOException {
//        isRunningInVagrant();
        // 測試容器狀態
//        boolean isRunning = isContainerRunning("ytdlp-container");
//        System.out.println("Container is running: " + isRunning);
        ytdlp();
    }



    private static void ytdlp() throws IOException {
        String url ="https://www.youtube.com/watch?v=CtOmuclq9oM";
        VideoDetails videoDetails = new VideoDetails();
        List<String> command = new ArrayList<>();
        command.add("vagrant");
        command.add("ssh");
        command.add("0d36b1c");
        command.add("-c");
        String dockerCommand = String.format("sudo docker compose run ytdlp --dump-json '%s' | jq '{id, title, ext, duration}'", url);
        command.add(dockerCommand);
        System.out.println(String.format("Executing command: %s", String.join(" ", command)));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // 合併標準錯誤流到標準輸出流
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            boolean insideJson = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.trim().startsWith("{")) {
                    insideJson = true;
                }

                if (insideJson) {
                    stringBuilder.append(line).append("\n");
                }

                if (insideJson && line.trim().endsWith("}")) {
                    break; // 找到完整的 JSON 就結束
                }

            }

            JSONObject json = new JSONObject(stringBuilder.toString());
            videoDetails.setVideoId(json.getString("id"));
            videoDetails.setTitle(cleanTitle(json.getString("title")));
            videoDetails.setExt(json.getString("ext"));
            videoDetails.setDuration(new BigDecimal(json.getString("duration")));
            // 超過10分鐘
            if(videoDetails.getDuration().compareTo(new BigDecimal(600)) > 0){
                videoDetails.setErrorMessage("video length too long");
            }
            System.out.println(videoDetails.toString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }

    private static String cleanTitle(String title) {
        return title.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static void isRunningInVagrant() {
        // 先測試 vagrant 是否可用
        try {
            Process p = Runtime.getRuntime().exec("vagrant --version");
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            System.out.println("Vagrant version: " + reader.readLine());
        } catch (Exception e) {
            System.out.println("Error checking vagrant: " + e.getMessage());
        }
    }

    public static boolean isContainerRunning(String containerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "vagrant", "ssh", "-c", "sudo docker inspect -f {{.State.Running}} " + containerName);

            pb.directory(new File(vagrantfile)); // 設置執行目錄到包含 Vagrantfile 的資料夾
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
                String output = reader.readLine();
                return "true".equalsIgnoreCase(output);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
