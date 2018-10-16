public interface TwitterAPI {
    void subscribe(String hashtag);

    void unsubscribe(String hashtag);

    void tweet(String msg);
}