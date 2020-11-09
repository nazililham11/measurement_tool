#include <ArduinoWebsockets.h>
#include <ESP8266WiFi.h>
#include <NewPing.h>

using namespace websockets;

const uint8_t TRIGGER_PIN       = 4;
const uint8_t ECHO_PIN          = 5;
const uint8_t MAX_DISTANCE      = 500;              //Cm
const uint8_t SCAN_TIMES        = 60;

const char* WIFI_SSID           = "TP-LINK";        //Enter SSID
const char* WIFI_PASSWORD       = "wifinerusak";    //Enter Password

const uint8_t WEBSOCKET_PORT    = 80;

WebsocketsServer Server;


// NewPing setup of pins and maximum distance.
NewPing Sonar(TRIGGER_PIN, ECHO_PIN, MAX_DISTANCE); 

void setup() {
	Serial.begin(115200);
	
	wifi_connect();
    websocket_init();
}

void loop() {
    while (Server.available()){
        
        WebsocketsClient client = Server.accept();
        client.onEvent(onEventsCallback);

        // run callback when messages are received
        client.onMessage([&](WebsocketsMessage message) {
        
            String data = String(message.data());
            Serial.print("Got Message: ");
            Serial.println(message.data());
        
            if (data.equals("ping")){
                unsigned int distance = sonar_read();
                client.send("Distance: " + String(distance));
                Serial.print("Send Message: ");
                Serial.println(distance); 
            }
            
        });

        while(client.available()){
            client.poll();
        }

    }
    delay(1000);
}


void onEventsCallback(WebsocketsEvent event, String data) {
    if(event == WebsocketsEvent::ConnectionOpened) {
        Serial.println("Connnection Opened");
    } else if(event == WebsocketsEvent::ConnectionClosed) {
        Serial.println("Connnection Closed");
    } else if(event == WebsocketsEvent::GotPing) {
        Serial.println("Got a Ping!");
    } else if(event == WebsocketsEvent::GotPong) {
        Serial.println("Got a Pong!");
    }
}

void wifi_connect(){
	WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

	// Wait some time to connect to wifi
	for(int i = 0; i < 15 && WiFi.status() != WL_CONNECTED; i++) {
        Serial.print(".");
        delay(1000);
	}
	
	Serial.println("");
	Serial.println("WiFi connected");
	Serial.println("IP address: ");
	Serial.println(WiFi.localIP());   //You can get IP address assigned to ESP

}

void websocket_init(){
	Server.listen(WEBSOCKET_PORT);
	Serial.print("Is Server live? ");
	Serial.println(Server.available());
}

unsigned int sonar_read() {
    unsigned long echoTime = Sonar.ping_median(SCAN_TIMES, MAX_DISTANCE);
    unsigned int distance = NewPing::convert_cm(echoTime);

    Serial.print("Ping: ");
    Serial.print(distance); 
    Serial.println("cm");

    return distance;
}