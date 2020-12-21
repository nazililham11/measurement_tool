#include <ESP8266WiFi.h>
#include <ESPAsyncTCP.h>
#include <ESPAsyncWebServer.h>
#include <NewPing.h>
#include <Wire.h> 
#include <LiquidCrystal_I2C.h>

const char* AP_WIFI_SSID            = "SoftAP";             //Access Point SSID
const char* AP_WIFI_PASSWORD        = "12345678";           //Access Point Password

const char* WIFI_SSID               = "MyWifi";             //Wifi SSID
const char* WIFI_PASSWORD           = "12345678";           //Wifi Password

const IPAddress ap_ipAddress        (10, 10, 10, 1);        //Server IP Address
const IPAddress ap_gateway          (10, 10, 10, 1);        //Server Gateway
const IPAddress ap_subnetMask       (255, 255, 255, 0);     //Server Subnetmask                                   

const uint8_t TRIGGER_PIN           = 12;
const uint8_t ECHO_PIN              = 14;
const uint8_t MAX_DISTANCE          = 600;                  //Cm
const uint8_t SCAN_TIMES            = 10;

const uint8_t WEBSOCKET_PORT        = 80;

const uint8_t LCD_ADDRESS           = 0x27;
const uint8_t LCD_COLS              = 16;
const uint8_t LCD_ROWS              = 2;

const int32_t DEBUG_BAUD_RATE       = 115200;
const uint8_t WIFI_CONNECT_TIMEOUT  = 15;                   //Detik

unsigned int distance; 
bool access_point_mode;

NewPing Sonar(TRIGGER_PIN, ECHO_PIN, MAX_DISTANCE); 
LiquidCrystal_I2C lcd(LCD_ADDRESS, LCD_COLS, LCD_ROWS);  

// Create AsyncWebServer object on port 80
AsyncWebServer server(80);
AsyncWebSocket ws("/");

void wifi_init();
void wifi_softap_start();
void lcd_init();
void lcd_show_distance();
void websocket_send_distance();
void websocket_handle_message(void *arg, uint8_t *data, size_t len);
void websocket_onEvent(AsyncWebSocket *server, AsyncWebSocketClient *client, AwsEventType type, void *arg, uint8_t *data, size_t len);
void websocket_init();
unsigned int sonar_read();
    

void setup(){
  // Serial port for debugging purposes
  Serial.begin(DEBUG_BAUD_RATE);
  
  lcd_init();
  wifi_init();
  websocket_init();

  // Start server
  server.begin();
}

void loop() {
    distance = sonar_read();
    lcd_update();

    Serial.println("Distance :"+String(distance));
    ws.cleanupClients();

    delay(100);
}

void wifi_init() {
    Serial.println("Try connect to WiFi \"" + String(WIFI_SSID) + "\"");
    WiFi.mode(WIFI_STA);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

    for (int i = 0; i < 15 && WiFi.status() != WL_CONNECTED; i++) {
        Serial.print(".");
        delay(1000);
    }
    Serial.println(""); 
    if (WiFi.status() != WL_CONNECTED){
        Serial.println("Cannot connect to Wifi");
        wifi_softap_start();
    } else if (WiFi.status() == WL_CONNECTED) {
        Serial.println("WiFi connected");
        Serial.print("IP address: ");
        Serial.println(WiFi.localIP());   //You can get IP address assigned to ESP
        access_point_mode = false;
    }
}

void wifi_softap_start() {
    Serial.print("Starting SoftAP");
    
    WiFi.mode(WIFI_AP);
    WiFi.softAPConfig(ap_ipAddress, ap_gateway, ap_subnetMask);   // subnet FF FF FF 00
    WiFi.softAP(AP_WIFI_SSID, AP_WIFI_PASSWORD);
    
    Serial.print("AP IP address: ");
    Serial.println(WiFi.softAPIP());
    Serial.println("Done");

    access_point_mode = true;
}

void lcd_init(){
    lcd.init();
    lcd.backlight();
    lcd.clear();
}

void lcd_update(){
    lcd.clear();
    lcd_set_distance();
    lcd_set_wifi();
}

void lcd_set_distance() {
    String text = "Jarak : " + String(distance) + " cm";
    lcd.setCursor(0, 0);
    lcd.print(text);
}

void lcd_set_wifi() {
    String text = "";
    lcd.setCursor(0, 1);
    if (access_point_mode){
        text = "AP |" + WiFi.softAPIP().toString();
    } else {
        text = "STA|" + WiFi.softAPIP().toString();
    }
    lcd.print(text);
}

unsigned int sonar_read() {
    unsigned long echoTime = Sonar.ping_median(SCAN_TIMES, MAX_DISTANCE);
    unsigned int distance = NewPing::convert_cm(echoTime);

    Serial.print("Ping: ");
    Serial.print(distance); 
    Serial.println("cm");

    return distance;
}

void websocket_send_distance_old() {
    String response = "Distance: " + String(distance);
    ws.textAll(response);
}

void websocket_send_distance() {
    String response = "{\"dst\":" + String(distance) + "}";
    ws.textAll(response);
}

void websocket_send_status() {
    String response = "{";
    if (access_point_mode){
        response += "\"mode\":\"AP\",";
        response += "\"ssid\":\"" + String(AP_WIFI_SSID) + "\",";
        response += "\"pass\":\"" + String(AP_WIFI_PASSWORD) + "\",";
        response += "\"ip\":\"" + WiFi.softAPIP().toString() + "\",";
    } else {
        response += "\"mode\":\"STA\",";
        response += "\"ssid\":\"" + String(WIFI_SSID) + "\",";
        response += "\"pass\":\"" + String(WIFI_PASSWORD) + "\",";
        response += "\"ip\":\"" + WiFi.localIP().toString() + "\",";
    }
    response += "\"uptime\":" + String(millis());   
    response += "}";       
    ws.textAll(response);
}

void websocket_handle_message(void *arg, uint8_t *data, size_t len) {
    AwsFrameInfo *info = (AwsFrameInfo*)arg;
    if (info->final && info->index == 0 && info->len == len && info->opcode == WS_TEXT) {
        data[len] = 0;
        if (strcmp((char*)data, "ping") == 0) {
            websocket_send_distance_old();
        } else if (strcmp((char*)data, "scan") == 0) {
            websocket_send_distance();
        } else if (strcmp((char*)data, "status") == 0) {
            websocket_send_status();
        }
    }
}

void websocket_onEvent(AsyncWebSocket *server, AsyncWebSocketClient *client, 
    AwsEventType type, void *arg, uint8_t *data, size_t len) {
    if (type == WS_EVT_CONNECT) {
        Serial.println("Connnection Opened");
        Serial.printf("WebSocket client #%u connected from %s\n", client->id(), client->remoteIP().toString().c_str());
    } else if (type == WS_EVT_DISCONNECT) {
        Serial.println("Connnection Closed");
        Serial.printf("WebSocket client #%u disconnected\n", client->id());
    } else if (type == WS_EVT_DATA) {
        Serial.println("Got a Data!");
        websocket_handle_message(arg, data, len);
    } else if (type == WS_EVT_PONG) {
        Serial.println("Got a Pong!");
    } else if (type == WS_EVT_ERROR) {
        Serial.println("Got a Error!");
    }
}

void websocket_init() {
    ws.onEvent(websocket_onEvent);
    server.addHandler(&ws);
}