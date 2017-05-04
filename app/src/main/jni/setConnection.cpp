#include "experiment_network_ipv4overipv6_MainActivity.h"
#include <cstring>
#include <android/log.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <cstdio>
struct Msg{
    int length;
    char type;
    char data[4096];
};

Msg* receivebuffer;

const jint server_port = 5678;
JNIEXPORT jint JNICALL Java_experiment_network_ipv4overipv6_MainActivity_setConnection(JNIEnv *env, jobject thiz){
    int sockfd;

    sockfd = socket(AF_INET6, SOCK_STREAM, 0);
    sockaddr_in6 dest_addr;
    in6_addr addr;
    dest_addr.sin6_family = AF_INET6;
    dest_addr.sin6_port = htons(server_port);
    addr.in6_u.u6_addr16[0] = 0x2402;
    addr.in6_u.u6_addr16[1] = 0xf000;
    addr.in6_u.u6_addr16[2] = 0x0001;
    addr.in6_u.u6_addr16[3] = 0x4417;
    addr.in6_u.u6_addr16[4] = 0x0000;
    addr.in6_u.u6_addr16[5] = 0x0000;
    addr.in6_u.u6_addr16[6] = 0x0000;
    addr.in6_u.u6_addr16[7] = 0x0900;
    dest_addr.sin6_addr = addr;
    Msg *msg = new Msg();

    msg->type = 100;
    msg->length = sizeof(Msg);
    int status = connect(sockfd, (struct sockaddr *)&dest_addr, sizeof(struct sockaddr));
    int so_error;
    socklen_t len = sizeof(so_error);

    getsockopt(sockfd, SOL_SOCKET, SO_ERROR, &so_error, &len);
    send(sockfd,msg,sizeof(msg),0);
    receivebuffer = new Msg();
    recv(sockfd,receivebuffer,4102,0);

    return receivebuffer->length;
}