import axios from "axios";

const http = axios.create({
    baseURL:'/localhost:8080/',
    timeout:3000
})

http.interceptors.request.use(
    (config)=>{
        const token = localStorage.getItem('token')
        if(token){
            config.headers["Authorization"] = `Bearer ${token}`
        }
        return config
    }

)

http.interceptors.response.use(
    (response)=>{
        return response.data
    },
    (error)=>{
        if(error.response){
            console.log("error"+error.response);
        }
        return Promise.reject(error)
    }
)