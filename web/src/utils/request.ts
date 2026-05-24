import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'

const rawRequest = axios.create({
  baseURL: '/api/v1',
  timeout: 10000
})

rawRequest.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

rawRequest.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

const request = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return rawRequest.get(url, config).then((response: AxiosResponse<T>) => response.data)
  },
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return rawRequest.post(url, data, config).then((response: AxiosResponse<T>) => response.data)
  },
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return rawRequest.put(url, data, config).then((response: AxiosResponse<T>) => response.data)
  },
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return rawRequest.delete(url, config).then((response: AxiosResponse<T>) => response.data)
  },
  raw: rawRequest
}

export default request
