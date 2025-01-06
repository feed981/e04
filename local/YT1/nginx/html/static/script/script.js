const { createApp } = Vue;

createApp({
    data() {
    return {
        url: '',
        isMP4: false,
        filename: '',
        downloadformat: 'mp3',
        isLoading: false, // loading style
        loadingPercentage: 0,
        errorMessage: '',
        successMessage: '',
        embedUrl: '',
    };
    },
    mounted() {
        this.connectws();
    //   const interval = setInterval(() => {
    //     if (this.loadingPercentage < 100) {
    //       this.loadingPercentage += 1; // 每秒增加 3%
    //     }
    //   }, 50); // 每秒更新一次
    },
    computed: {
    format() {
        return this.isMP4 ? 'mp4' : 'mp3';
    }
    },
    methods: {
        
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
        isValidYouTubeURL(url) {
            const youtubeRegex = /^(?:https?:\/\/)?(?:www\.)?(?:youtu\.be\/|youtube\.com\/(?:embed\/|v\/|watch\?v=|watch\?.+&v=))((\w|-){11})(?:\S+)?$/;
            return youtubeRegex.test(url);
        },
        async convert() {
            this.errorMessage = '';
            this.successMessage = '';

            if (!this.url ||!this.isValidYouTubeURL(this.url)) {
                this.errorMessage = 'Please enter a valid YouTube URL';
                this.successMessage = '';
                return;
            }
            
        //  test:
        //        this.downloadformat = this.isMP4 ? 'mp4' : 'mp3';
        //   this.downloadUrl = this.url;

            try {
                this.isLoading = true;
                this.filename = '';
                this.loadingPercentage = 0;
                const response = await axios.post('/YT1/convert', {
                    url: this.url,
                    format: this.format
                });

                if (response.data === 'ok' || response.data === 'exist') {
                    this.downloadformat = this.isMP4 ? 'mp4' : 'mp3';
                    this.errorMessage = '';
                    this.successMessage = `Your ${this.format.toUpperCase()} conversion is in progress. A download link will be available shortly!`;
                } else {
                    throw new Error('Invalid response from server');
                }
            } catch (error) {
                console.error(error);
                alert('An error occurred during the conversion. Please try again.');
            } finally {
                this.isLoading = false
            }
        },
        async downloadMP3() {
            try {
                const response = await axios.get('/YT1/download', {
                    params: { 
                        filename: this.filename,
                        date: new Date().toISOString(),
                    },
                    responseType: 'blob' // Important for downloading files
                });
                // 創建下載鏈接
                const url = window.URL.createObjectURL(new Blob([response.data]));
                const link = document.createElement("a");
                link.href = url;
                link.setAttribute("download", this.filename); // 設置文件名
                link.click();

                // 釋放 URL 資源
                window.URL.revokeObjectURL(url);
            } catch (error) {
                console.error("下載失敗：", error);
            }
        },
    }
}).mount('#app');