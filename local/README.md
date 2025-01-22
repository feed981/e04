2024-12-18 ~ 2024-12-19
start

vagrant docker compose 
nacos nginx mysql redmine
從hmtt學來的基本配置

Vagrantfile 定義虛擬機的配置
在虛擬機內部安裝 Docker 和 Docker Compose
使用 docker-compose.yml 文件來定義應用的多個服務

frontend 
先用chatgpt跟 https://websim.ai/ 各產了一個
改成vue

backend
gateway-filter 
service-

---

2024-12-30 ~ 2024-12-31
window 工具：下載 ytdlp、轉換 ffmpeg
frontend
想增加做下載中轉圈、可以選擇mp3、mp4

---

2025-01-01 
ytdlp 文字編碼問題
backend 下載轉換做異步處理，等太久
請求可以更快回去然後先給用戶一個訊息說，正在下載、轉換中
```javascript 

await axios.post('/yt1/convert', {
                url: this.url,
                format: this.format
            }).then(response => {
                if (response.data.success === true) {
                    this.downloadformat = this.isMP4 ? 'mp4' : 'mp3';
                    this.errorMessage = '';
                    this.successMessage = `Your ${this.format.toUpperCase()} conversion is in progress. A download link will be available shortly!`;
                
                }else if(response.data.success === false){
                    this.errorMessage = response.data.message;
                    this.successMessage = '';
                }
```
一開始是用 Executors.newSingleThreadExecutor()

關於大任務小任務：
```
RabbitMQ：适用于 大规模任务处理 和 分布式架构。
Executors.newSingleThreadExecutor()：适用于 小规模任务 和 本地处理。

convert() 屬於大任務
基於以下原因：
資源消耗高：轉換操作需要消耗大量 CPU 和 I/O 資源。
運行時間不確定：處理視頻長短和解析度不同，運行時間可能變化較大。
可能的併發需求：如果需要處理多個視頻，系統負載會迅速增長。
```
所以就改RabbitMQ

---

2025-01-02

RabbitMQ 還蠻好上手的

backend 基本配置
```java
//-- configuration file --
rabbitmq:
  user-log-queue: userLogQueue

//-- RabbitMQConfig --
@Bean
public Queue userLogQueue() {);
    return new Queue("userLogQueue", true);
}

//-- Listener --
@RabbitListener(queues = "${rabbitmq.user-log-queue}")
public void handleUserLog(UserLog userLog) {
    log.info("userLog:{}",userLog);
    // 後續處理
}

//-- send q --
rabbitTemplate.convertAndSend("userLogQueue", "your T");

//-- RabbitResponse - send q and catch --
rabbitResponse.queueMessageLog("userLogQueue", "your T");

```
主要分成
downloadQueue 下載任務
convertQueue 轉換任務完成後，發消息給通知任務

---

2025-01-03 ~ 2025-01-05

Gateway 使用 WebFlux，而後端服務使用 Spring MVC（Web）
一開始是打算在 Gateway filter 存download log 的數據到數據庫
WebFlux 要調用其他包遇到一些問題搞很久(比如循環依賴)
把後端服務改成 WebFlux後來發現ws 傳不過來，因為對ws也不熟所以最後又改回Web
睡覺起來在想到一開始就弄錯了
業務邏輯完全不該在Gateway這邊做

WebFlux與Web：
```
Spring Cloud Gateway 基於 WebFlux，
它是反應式編程模型，適合處理高併發和非阻塞的 I/O 操作。

Spring MVC 是傳統的基於 Servlet 的同步模型，
更適合處理阻塞操作，並且與大多數現有的 Spring 應用程式兼容。
```
併發和阻塞：
```
高併發是同時有很多請求
一個網站同時有很多人訪問，就是高併發的場景。

阻塞和非阻塞是處理這些請求時，系統是否讓請求排隊等待資源
阻塞：一個請求在等待資源（比如數據庫查詢、檔案讀寫）完成之前，會一直佔用系統資源。
這樣會導致其他請求需要排隊等待，尤其在高併發情況下，可能會出現長時間等待。

非阻塞：請求發出後，不會等待資源完成，而是立即返回，讓系統可以繼續處理其他請求。
當資源準備好後，再通知請求繼續。這樣可以更高效地利用資源，特別是在高併發場景下。
```
downloadLogQueue 下載任務前先將存數據

---

2025-01-06

nginx
upstream: ws

```
    location /ws {
        proxy_pass http://ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host $host;
        
        # 支持 SockJS 的长轮询和其他请求
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
```

backend
bucket4j downloadLimiter
先有這個想法
防止用戶一直按下載，後來改成redis

RabbitMQ
notificationQueue 转换通知
embedUrlQueue 嵌入网址


WebSocket
/topic/convert 转换通知
/topic/embedUrl 嵌入网址

```javascript

        connectws() {
            const socket = new SockJS('http://localhost:8801/ws'); // Nginx 或直接访问 Spring Boot 地址
            const client = webstomp.over(socket);
            
            client.connect(
                {},
                () => {
                    console.log("Connected to WebSocket");
                    // 订阅主题
                    client.subscribe("/topic/convert", (message) => {
                        this.filename = message.body;
                        this.successMessage = `You can download now! Please check your browser's download folder.`;
                        console.log("Received message: ", message.body);
                    });
                    client.subscribe("/topic/embedUrl", (message) => {
                        this.embedUrl = message.body;
                        console.log("Received message: ", message.body);
                    });
                },
                (error) => {
                    console.error("Connection error: ", error);
                }
            );
            
        },
```
主要就是在處理 convertQueue 轉換任務完成後，發消息給通知任務
```html 
<button v-if="filename" @click="downloadMP3">Download {{ downloadformat.toUpperCase() }}</button>
        <div class="error">
          {{ errorMessage }}
      </div>
      <div class="success">
        {{ successMessage }}
    </div>
```

跟一開始拿到影片連結，嵌入网址給用戶等待下載跟轉換任務還沒有下載按鈕的時候可以先看下影片
```html
<div v-if="embedUrl" class="embedUrl">
      <iframe :src="embedUrl" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>
    </div>
```

---

2025-01-08

mybatis 改 JPA
ApiResponse 統一返回 ResponseEntity 比較好看

防止用戶一直按下載 30秒才能再按一次下載
redis
```java
String requestHash = hashUtils.generateRequestHash(ip + ":" + filename);

if (!downloadLimiter.tryDownload(requestHash)) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.TOO_MANY_REQUESTS);
        }

        Boolean isSet = stringRedisTemplate.opsForValue().setIfAbsent(requestHash, String.valueOf(now),
                DUPLICATE_REQUEST_INTERVAL_MS, TimeUnit.MILLISECONDS);
        // 如果 Redis 中已存在相同键（且未过期），则请求重复。如果键不存在，则记录请求并允许通过
        return !Boolean.FALSE.equals(isSet);
```

---

2025-01-11 ~ 2025-01-12

打包到docker compose 測試

---

2025-01-13 ~ 2025-01-14

AWS 
IAM 

EC2

---

2025-01-15

yt-dlp 和 ffmpeg 从 Windows 切换到 Linux 环境
@Autowired 改 构造函数注入

```
@Autowired 提供了更簡單的注入方式，但可能隱藏了依賴關係。
構造函數注入 提供了更好的可控性和測試性，
```

添加 user_logs 表来存储用户位置数据并与 download_logs 链接，user_id 建立外键关系


---

2025-01-17

測試yt-dlp 和 ffmpeg 从 Windows 切换到 Linux 环境

```bash
# json get video detail
sudo docker compose run ytdlp --dump-json '%s' | jq '{id, title, ext, duration}'

# ytdlp 會自動調用ffmpeg mp4
sudo docker compose run ytdlp -- yt-dlp --config-location /config/yt-dlp.conf -o '%s'

# ffmpeg mp3
sudo docker compose run --rm ffmpeg -i '/downloads/%s.mp4' -q:a 0 -map a '/downloads/%s.mp3
```
cat ~/config/yt-dlp.conf
```conf
--output /downloads/%(title)s.%(ext)s
--format "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best"
--merge-output-format mp4
```
---

2025-01-19
queue 裡面的方法整理一下
下載前檢查檔案 mp4
mp3才要轉換
轉換完發消息通知

---

2025-01-20 ~ 2025-01-21

jwt
一開始是透過IP取經緯度
取經緯度主要想練習geo yt總部的距離因為挺搞笑但後來沒做
request經過反向代理都會取到自己內網IP
所以需要打api1取得用戶外網IP
然後再透過api2用外網IP取得location
但這樣每次進網站因為不知道是誰都要先打兩個api
後來為了身分辨識所以加上jwt
gateway filter set header user-id 到下游

```java 
// 修改 request，加入 user id
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header(USER_ID_HEADER, userId)
                        .build();
```
service 在去set ThreadLocal 
不過有遇到queue+ThreadLocal問題
這邊在service filter 先打api1取得用戶外網IP 存redis
打userId丟進queue 
要透過api2用外網IP取得location
因為不同進程 queue 這邊在set ThreadLocal 進到接下來的方法就get得到了

ttl是10hr
關閉瀏覽器後，localStorage 內的token不會消失，除非是無痕模式，或用戶手動清除
