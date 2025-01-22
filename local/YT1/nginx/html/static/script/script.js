const { createApp } = Vue;

createApp({
    data() {
    return {
        url: '',
        isMP4: false,
        filename: '',
        downloadformat: 'mp3',
        isLoading: false, // loading style
        errorMessage: '',
        successMessage: '',
        embedUrl: '',
    };
    },
    mounted() {// 在组件渲染完成后执行
        this.checkOrGenerateToken();
        this.connectws();
    },
    computed: {
    format() {
        return this.isMP4 ? 'mp4' : 'mp3';
    }
    },
    methods: {
        // 頁面載入時檢查是否已有token,沒有則請求新的
        async checkOrGenerateToken() {
            let token = localStorage.getItem('token');
    
            if (!token) {
                try {
                    const response = await axios.get('/auth/token');
                    token = response.data;
                    localStorage.setItem('token', token);
                } catch (error) {
                    console.error('無法獲取 token:', error);
                }
            }
            
            // 設置 axios 攔截器
            axios.interceptors.request.use(config => {
                const token = localStorage.getItem('token');
                if (token) {
                    config.headers.Authorization = `Bearer ${token}`;
                }
                return config;
            });
        },
        connectws() {
            const socket = new SockJS('http://localhost:8080/ws'); // Nginx 或直接访问 Spring Boot 地址
            const client = webstomp.over(socket);
            
            client.connect(
                {},
                () => {
                    console.log("Connected to WebSocket");
                    // 订阅主题
                    client.subscribe("/topic/convert", (message) => {
                        this.isLoading = false;
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
            this.embedUrl = '';
            this.filename = '';
            this.isLoading = true;

            if (!this.url ||!this.isValidYouTubeURL(this.url)) {
                this.errorMessage = 'Please enter a valid YouTube URL';
                this.successMessage = '';
                this.isLoading = false;
                return;
            }
            
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
            }).catch(error => {
                if (error.response) {
                    // 服务器返回的响应数据
                    this.errorMessage = '服务器内部错误';
                    this.embedUrl = '';
                    console.error('Request failed', error.response.data);
                } else if (error.request) {
                    // 请求已发出但没有收到响应
                    console.error('No response received', error.request);
                } else {
                    // 其他错误，如设置请求时触发的错误
                    console.error('Error', error.message);
                }
            }).finally(() => { 
                // this.isLoading = false
            });

        },
        async download() {
            await axios.get('/yt1/download', {
                params: { 
                    filename: this.filename,
                    // date: new Date().toISOString(),
                },
                responseType: 'blob' // Important for downloading files
            }).then(async response => {
                if (response.headers['content-type'] === 'application/json') {
                    // 將 blob 轉為 JSON
                    const text = await response.data.text();
                    const json = JSON.parse(text);
                    if (json.success === false) {
                        this.errorMessage = json.message;
                        this.successMessage = '';
                        return;
                    }

                }else{
                    // 創建下載鏈接
                    const url = window.URL.createObjectURL(new Blob([response.data]));
                    const link = document.createElement("a");
                    link.href = url;
                    link.setAttribute("download", this.filename); // 設置文件名
                    link.click();
    
                    // 釋放 URL 資源
                    window.URL.revokeObjectURL(url);
                }
            }).catch(error => {
                if (error.response) {
                    // 服务器返回的响应数据
                    this.errorMessage = 'You have reached the download limit. Please try again later.';
                    this.successMessage = '';
                } else if (error.request) {
                    // 请求已发出但没有收到响应
                    console.error('No response received', error.request);
                } else {
                    // 其他错误，如设置请求时触发的错误
                    console.error('Error', error.message);
                }
            });
        },
    }
}).mount('#app');