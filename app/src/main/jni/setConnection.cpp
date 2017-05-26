#include "experiment_network_ipv4overipv6_MainActivity.h"
#include <android/log.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <sys/socket.h>
#include <resolv.h>
#include <stdlib.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <pthread.h>

#define BUFSIZE 10240

#define FIFO "/data/data/experiment.network.ipv4overipv6/files/tmp/FLOWFIFO.txt"
#define FIFO2 "/data/data/experiment.network.ipv4overipv6/files/tmp/IPFIFO.txt"
#define FIFO3 "/data/data/experiment.network.ipv4overipv6/files/tmp/VPNFIFO.txt"
#define TAG "TAG"
char* IP = "2402:f000:1:4417::900";
//char* IP = "2402:f000:2:7001:49b6:eb24:cb0f:f0e8";
//char* IP = "2402:f000:ffff:64:814b:bc7f:e306:cee9";
char* port = "5678";
int heart = 0;//心跳包
int upload_packages= 0;//接收的包数目
int download_packages = 0;//发送达到包数目
int upload_size = 0;//接收的数据大小
int download_size = 0;//发送的数据大小
int download_speed = 0;
int upload_speed = 0;
int read_times = 0;//读取次数
int sockfd;//套接字
int seconds = 0;//心跳包秒数
int tunfd;

int virtual_interface;//虚接口
char cri[BUFSIZE];
struct Msg{
    int length;
    char type;
    char data[4096];
};

Msg* receivebuffer;

/*
        读取虚接口线程
        A. 持续读取虚接口；
    	B. 记录读取的长度和次数；
    	C. 封装102(上网请求)类型的报头；
    	D. 通过IPV6套接字发送给4over6隧道服务器。
*/

void* virtual_thread(void* args){
    //char buf[BUFSIZE];
    __android_log_print(ANDROID_LOG_ERROR, "Tag","enter_virtual_thread");
    /*封装102类型报头（上网请求）*/
    Msg msg2;//新消息结构体
    char buffer[BUFSIZE+1];//缓存区
    /*持续读取虚接口*/
     //    int file_handle = open(FIFO3,O_RDONLY);
    while(1){
        memset(buffer,0,BUFSIZE);


       int l = read(tunfd,buffer,sizeof(buffer));
       __android_log_print(ANDROID_LOG_ERROR, "Read","length:%d ; info:%s",l,buffer);
        if(l>=0){//读取成功
            /*完成统计情况(读取长度和次数）*/
            upload_size += l;
            upload_packages ++;
            upload_speed += l;
                     memcpy(msg2.data,buffer,sizeof(msg2.data));
                                msg2.type = 102;
                                msg2.length = sizeof(msg2);
                                memset(buffer,0,BUFSIZE);
                                memcpy(buffer,&msg2,sizeof(msg2));
                                int len = send(sockfd,buffer,sizeof(buffer),0);
                  //  __android_log_print(ANDROID_LOG_ERROR, "103length",":%d",len);
        }
    }
}

/*定时器线程（间隔1秒）：
	1、读写虚接口的流量信息写入管道；
	2、获取上次收到心跳包距离当前时间的秒数S；
	3、假如S大于60，说明连接超时，就关闭套接字；
	4、S小于60就每隔20秒给服务器发送一次心跳包。
*/

void * clock_thread(void* args){
__android_log_print(ANDROID_LOG_ERROR, "Tag","enter_clock_thread");
    int time = 0;
    if(access(FIFO2,F_OK) == -1){//无法达到
        if(mknod(FIFO2,S_IFIFO | 0666,0) != 0){//创建有名通道
            //exit(-3);
        }
    }

    /*以下进行循环*/
    while(1){
        sleep(1);//间隔1秒
        __android_log_print(ANDROID_LOG_ERROR, "seconds","%d",heart);
        heart ++; // 心跳包个数
        seconds ++; //心跳包秒数
        /*将所有的信息保存在文件中*/
        char temp1[100];
        char temp2[100];
        char temp3[100];
        char temp4[100];
        char temp5[100];
        char temp6[100];
        sprintf(temp1,"%d",upload_size);
        sprintf(temp2,"%d",upload_speed);
        sprintf(temp3,"%d",download_size);
        sprintf(temp4,"%d",download_speed);
        sprintf(temp5,"%d",upload_packages);
        sprintf(temp6,"%d",download_packages);
        strcat(temp1," ");
        strcat(temp1,temp2);
        strcat(temp1," ");
        strcat(temp1,temp3);
        strcat(temp1," ");
        strcat(temp1,temp4);
        strcat(temp1," ");
        strcat(temp1,temp5);
        strcat(temp1," ");
        strcat(temp1,temp6);
        strcat(temp1," ");
        int file_handle = open(FIFO,O_WRONLY|O_CREAT|O_TRUNC);

        download_speed = 0;
        upload_speed = 0;
        write(file_handle,temp1,1000);
        close(file_handle);

        /*以下检测心跳*/
        if(heart > 60){//超时，应该关闭链接
            close(sockfd);
        }

        else if(seconds == 20){//每20秒需要发送一个心跳包
            seconds = 0;
            Msg msg;
            msg.length = 5;
            msg.type = 104;//心跳包
            memset(msg.data,0,sizeof(msg.data));
            char buf[BUFSIZE];
            memset(buf,0,sizeof(buf));
            memcpy(buf,&msg,sizeof(msg));
            int len = send(sockfd,buf,5,0);//发送心跳包
            int type = (int)*(char*)(buf+4);
            __android_log_print(ANDROID_LOG_ERROR, "send104",":%d",type);
            __android_log_print(ANDROID_LOG_ERROR, "send104",":%d",len);
        }
    }
}

const jint server_port = 5678;
JNIEXPORT jint JNICALL Java_experiment_network_ipv4overipv6_MainActivity_setConnection(JNIEnv *env, jobject thiz, jstring dir){

    //首先我们需要创建ipv6套接字
    int flag = 0;
    __android_log_print(ANDROID_LOG_ERROR, "Tag","start");
    sockfd = socket(AF_INET6, SOCK_STREAM, 0);//AF_INET指ipv6,而SOCK_STREAM指TCP
    if(sockfd < 0){//创建失败
        exit(-1);//直接退出

    }
    //fcntl(sockfd,F_SETFL, O_NONBLOCK);
    /*以下操作将地址信息等放到了dest_addr中*/
    sockaddr_in6 dest_addr;

    bzero(&dest_addr, sizeof(dest_addr));//将dest所指内存区域清零
        dest_addr.sin6_family = AF_INET6;
        dest_addr.sin6_port = htons(atoi(port));
        __android_log_print(ANDROID_LOG_ERROR, "Tag","start3");
        if ( inet_pton(AF_INET6, IP, &dest_addr.sin6_addr) < 0 ) {//这个函数转换字符串到网络地址，第一个参数是地址簇，第二个参数是来源地址，第三个参数接收转换后的数据

            __android_log_print(ANDROID_LOG_ERROR, "Tag","inetfailed");
        }
    /*尝试连接*/
    int status = connect(sockfd, (struct sockaddr *)&dest_addr, sizeof(dest_addr));//尝试连接
        __android_log_print(ANDROID_LOG_ERROR, "Tag","status=%d",status);
    Msg msg;//新消息结构体
    msg.type = 100;//IP请求信息
    msg.length =5;
    //memset(msg.data,0,sizeof(msg.data));
    char buffer[BUFSIZE+1];//缓存区
    memset(buffer,0,BUFSIZE);
    //  __android_log_print(ANDROID_LOG_ERROR, "Tag","start22");
    memcpy(buffer,&msg,sizeof(msg));
    int type = (int)*(char*)(buffer+4);
    __android_log_print(ANDROID_LOG_ERROR, "send100",":%d",type);
  //    __android_log_print(ANDROID_LOG_ERROR, "Tag","start23");
    int len = send(sockfd,buffer,5,0);//发送

     __android_log_print(ANDROID_LOG_ERROR, "send","length %d",len);


    /*创建定时器线程*/

    pthread_t time_thread;//新线程

    pthread_create(&time_thread,NULL,clock_thread,NULL);//创建定时器线程




    while(true){

            char buf1[BUFSIZE];
            memset(buf1,0,sizeof(buf1));
            int length = recv(sockfd,buf1,BUFSIZE,0);
            __android_log_print(ANDROID_LOG_ERROR, "flag","here");
            __android_log_print(ANDROID_LOG_ERROR, "recv","");
            Msg mesg;
            memset(mesg.data,0,sizeof(mesg.data));
            memcpy(&mesg,buf1,sizeof(mesg));
           // int length = 10;
            if(length>0){//认为信息有效

                __android_log_print(ANDROID_LOG_ERROR, "type","%d",mesg.type);

                download_packages ++;
                download_size += length;
                download_speed += length;

                if(mesg.type == 101 && flag == 0){//&&first_time_ip){
                    flag = 1;
                    int file_handle = open(FIFO2,O_WRONLY);

                    write(file_handle,mesg.data,sizeof(mesg.data));
                    close(file_handle);
                     char buf[17];
                     memset(buf,0,sizeof(buf));
                     sleep(5);
                    /*写入管道*/

                   file_handle = open(FIFO3,O_RDONLY);
                   int len = read(file_handle,buf,sizeof(buf));
                   for(int i=0;i<4;++i)
                    __android_log_print(ANDROID_LOG_ERROR, "tun buffer is","%d",buf[i]);
                  // if(len > 0)
                   close(file_handle);


                    tunfd = *(int*)buf;
                     __android_log_print(ANDROID_LOG_ERROR, "tunfd","%d",tunfd);
                    pthread_t virtualThread;
                    pthread_create(&virtualThread,NULL,virtual_thread,NULL);
                }

                else if(mesg.type == 103){
                    int length = write(tunfd,mesg.data,mesg.length);
                    read_times ++;
                    download_packages += length;
                }


                else if(mesg.type == 104){
                    heart = 0;
                }
            }
      }

    return 0;
}