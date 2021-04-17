#include <ESP8266WiFi.h>				// Untuk menghubungkan dengan WIFI atau membuat WIFI access point
#include <ESPAsyncTCP.h>				// Untuk melakukan komunikasi TCP (protokol komunikasi pada jaringan wifi)
#include <ESPAsyncWebServer.h>			// Untuk menjalankan server WebSocket pada mikrokontroler
#include <Wire.h> 						// Untuk berkomunikasi melalui protokol I2C (protokol kamunikasi untuk modul LCD dan modul PCF8591)
#include <LiquidCrystal_I2C.h>			// Untuk berkomunikasi dengan modul LCD dan mempermudah mengoperasikan modul LCD

#define OUT_OF_RANGE 0
#define PCF8591 (0x90 >> 1)
#define DEBUG_BAUD_RATE 115200


// ----------------------------------------------------------------------------------------
// Variabel Konfigurasi WIFI
// ----------------------------------------------------------------------------------------
const char* AP_WIFI_SSID            = "SoftAP";             // Access Point SSID
const char* AP_WIFI_PASSWORD        = "12345678";           // Access Point Password

const char* WIFI_SSID               = "Redmi";              // Wifi SSID
const char* WIFI_PASSWORD           = "12345678";           // Wifi Password

const IPAddress ap_ipAddress        (10, 10, 10, 1);        // Server IP Address
const IPAddress ap_gateway          (10, 10, 10, 1);        // Server Gateway
const IPAddress ap_subnetMask       (255, 255, 255, 0);     // Server Subnetmask                                   

const uint8_t WIFI_CONNECT_TIMEOUT  = 15;                   // Detik

const uint8_t WEBSOCKET_PORT        = 80;



// ----------------------------------------------------------------------------------------
// Variabel Konfigurasi LCD 
// ----------------------------------------------------------------------------------------
const uint8_t LCD_ADDRESS           = 0x27;
const uint8_t LCD_COLS              = 16;
const uint8_t LCD_ROWS              = 2;




// ----------------------------------------------------------------------------------------
// Deklarasi Variable, Function, Object
// ----------------------------------------------------------------------------------------

unsigned int distance; 
float voltage;
bool access_point_mode;

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
uint8_t pcf8591_read();
uint16_t read_distance_cm();
uint16_t to_infrared_distance(uint16_t value);
uint16_t read_median(uint8_t it);   





// ----------------------------------------------------------------------------------------
// Inisialisasi Program
// ----------------------------------------------------------------------------------------
void setup(){

    // Memulai Komunikasi Serial untuk keperluan Debug
    Serial.begin(DEBUG_BAUD_RATE);
  
    
    lcd_init();         // Memulai komunikasi dengan modul LCD 
    wifi_init();        // Inisialisasi WIFI
    websocket_init();   // Inisialisasi Protokol Websocket

    // Memulai Server Websocket
    server.begin();
}




// ----------------------------------------------------------------------------------------
// Program Loop
// ----------------------------------------------------------------------------------------
void loop() {

    // Baca Jarak Sebanyak 100 Kali
    distance = read_median(100);

    // Update Tampilan LCD 
    lcd_update();


    // Tampilkan Pada Konsol Serial untuk proses Debug
    Serial.print("Distance :"+String(distance)+"cm, ");
    Serial.print("Voltage :"+String(voltage));
    Serial.println();

    // Cek apakah aplikasi meminta data jarak
    ws.cleanupClients();

    // Tunda 100 ms
    delay(100);
}





// ----------------------------------------------------------------------------------------
// Inisialisasi WIFI
// ----------------------------------------------------------------------------------------
void wifi_init() {

    // Tampilkan pesan pada LCD dan Konsol Serial
	lcd.clear();
	lcd_print("Try connect to", 0, 0);
    Serial.println("Try connect to WiFi \"" + String(WIFI_SSID) + "\"");

    // Hubungkan ke wifi 
    WiFi.mode(WIFI_STA);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

    // Tunggu sesuai nilai timeout serta update tampilan LCD
    for (int i = 0; i < WIFI_CONNECT_TIMEOUT && WiFi.status() != WL_CONNECTED; i++) {
        Serial.print(".");
        delay(1000);
        
        if (i % 3 == 0){
        	lcd_print(String(WIFI_SSID) + ".  ", 0, 1);
        } else if (i % 3 == 1) {
        	lcd_print(String(WIFI_SSID) + ".. ", 0, 1);
        } else {
        	lcd_print(String(WIFI_SSID) + "...", 0, 1);
        }

    }
    Serial.println(); 

    // Apabila wifi gagal terhubung 
    if (WiFi.status() != WL_CONNECTED){

        // Tamplkan pesan pada LCD dan konsol serial
    	lcd.clear();
		lcd_print("Cannot connect", 0, 0);
		lcd_print("to "+ String(WIFI_SSID), 0, 1);
		
		delay(1500);

        Serial.println("Cannot connect to Wifi");

        // Beralih ke mode Access Point 
        wifi_softap_start();


    // Apabila wifi berhasil terhubung
    } else if (WiFi.status() == WL_CONNECTED) {

        // Tampilkan pesan pada LCD dan konsol serial
    	lcd.clear();
		lcd_print("Connected to", 0, 0);
		lcd_print(String(WIFI_SSID), 0, 1);
		
		delay(1500);

        Serial.println("WiFi connected");
        Serial.print("IP address: ");
        Serial.println(WiFi.localIP());   //You can get IP address assigned to ESP
        access_point_mode = false;
    }
}




// ----------------------------------------------------------------------------------------
// Memulai Mode Access Point
// ----------------------------------------------------------------------------------------
void wifi_softap_start() {
    
    // Tampilkan pesan pada LCD dan konsol serial
    lcd.clear();
	lcd_print("Starting SoftAP", 0, 0);
	lcd_print(AP_WIFI_SSID, 0, 1);
    
    Serial.print("Starting SoftAP");
	
	delay(1500);
    
    // Nyalakan Access Point
    WiFi.mode(WIFI_AP);
    WiFi.softAPConfig(ap_ipAddress, ap_gateway, ap_subnetMask);   // subnet FF FF FF 00
    WiFi.softAP(AP_WIFI_SSID, AP_WIFI_PASSWORD);
    
    // Tampilkan pesan pada LCD dan konsol serial
	lcd.clear();
	lcd_print("W:"+String(AP_WIFI_SSID), 0, 0);
	lcd_print("P:"+String(AP_WIFI_PASSWORD), 0, 1);

    Serial.print("AP IP address: ");
    Serial.println(WiFi.softAPIP());
    Serial.println("Done");

    access_point_mode = true;

    delay(3000);
}



// ----------------------------------------------------------------------------------------
// Inisialisasi LCD 
// ----------------------------------------------------------------------------------------
void lcd_init(){
    lcd.init();         // Memulai komunikasi dengan modul LCD 
    lcd.backlight();    // Menyalakan Backlight
    lcd.clear();        // Bersihkan tampilan LCD 
}



// ----------------------------------------------------------------------------------------
// Memperbarui tampilan pada LCD 
// ----------------------------------------------------------------------------------------
void lcd_update(){
	String text = "";

    // Tampilan jarak normal
	if (distance != OUT_OF_RANGE)
    text = "Jarak: " + String(distance) + " cm        ";
    
    // Tampilan jarak apabila diluar batas pengukuran
    else text = "Jarak: Out Range";
    
    // Tampilkan text jarak pada baris 0 kolom 0
    lcd_print(text, 0, 0);



    // Tampilan status wifi dan ip address saat mode access point
    if (access_point_mode)
    text = "AP|" + WiFi.softAPIP().toString() + "    "; 
    
    // Tampilan status wifi dan ip address saat mode station
    else text = "ST|" + WiFi.localIP().toString() + "    ";

    // Tampilkan status wifi dan ip address pada kolom 0 baris 1
    lcd_print(text, 0, 1);

}



// ----------------------------------------------------------------------------------------
// Menampilkan pesan pada LCD 
// ----------------------------------------------------------------------------------------
void lcd_print(String str, uint8_t col, uint8_t row) {
    lcd.setCursor(col, row);    // Pindahkan kursor 
    lcd.print(str);             // Tampilkan text
}




// ----------------------------------------------------------------------------------------
// Protokol websocket untuk mengirimkan data jarak ke aplikasi 
// ----------------------------------------------------------------------------------------
void websocket_send_distance() {
    String response = "{\"dst\":" + String(distance) + "}";
    ws.textAll(response);
}



// ----------------------------------------------------------------------------------------
// Protokol websocket untuk mengirimkan data status device ke aplikasi
// ----------------------------------------------------------------------------------------
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


// ----------------------------------------------------------------------------------------
// Protokol websocket untuk mengirimkan data jarak ke aplikasi 
// ----------------------------------------------------------------------------------------
void websocket_handle_message(void *arg, uint8_t *data, size_t len) {
    AwsFrameInfo *info = (AwsFrameInfo*)arg;
    if (info->final && info->index == 0 && info->len == len && info->opcode == WS_TEXT) {
        data[len] = 0;

        // Apabila menerima data yang berisi perintah 'scan' 
        if (strcmp((char*)data, "scan") == 0) 
        websocket_send_distance();      // Kirimkan data jarak
        
        // Apabila menerima data yang berisi perintah 'status' 
        else if (strcmp((char*)data, "status") == 0) 
        websocket_send_status();        // Kirimkan data status
    }
}



// ----------------------------------------------------------------------------------------
// Protokol websocket saat terjadinya event (dibuka/ditutupnya komunikasi, menerima data, terjadi error)
// ----------------------------------------------------------------------------------------
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
    } else if (type == WS_EVT_ERROR) {
        Serial.println("Got a Error!");
    }
}



// ----------------------------------------------------------------------------------------
// Inisialisasi protokol websocket
// ----------------------------------------------------------------------------------------
void websocket_init() {
    ws.onEvent(websocket_onEvent);
    server.addHandler(&ws);
}


// ----------------------------------------------------------------------------------------
// Pembacaan nilai adc pada modul PCF8591
// ----------------------------------------------------------------------------------------
uint8_t pcf8591_read() {
    uint8_t adcvalue0, adcvalue1, adcvalue2, adcvalue3;

    // Mulai komunikasi dengan modul PCF 
    Wire.beginTransmission(PCF8591);
    Wire.write(0x04);
    Wire.endTransmission();
    Wire.requestFrom(PCF8591, 5);
    
    // Baca nilai jarak
    adcvalue0 = Wire.read();
    adcvalue0 = Wire.read();
    adcvalue1 = Wire.read();
    adcvalue2 = Wire.read();
    adcvalue3 = Wire.read();

    // Ambil nilai adc0 karena sensor terhubung ke pin adc0 (A0)
    return adcvalue0;
}


// ----------------------------------------------------------------------------------------
// Membaca jarak dengan satuan cm 
// ----------------------------------------------------------------------------------------
uint16_t read_distance_cm(){

    // Baca nilai sensor dari modul pcf
    uint16_t adc = pcf8591_read();

    // Hitung tegangan dari nilai adc
    voltage = (adc * 5) / 255.0;

    // Konversi nilai adc menjadi nilai jarak satuan cm
    return to_infrared_distance(adc);
}


// ----------------------------------------------------------------------------------------
// Mengkonversi nilai adc mencjadi nilai jarak (cm)
// ----------------------------------------------------------------------------------------
uint16_t to_infrared_distance(uint16_t value){

    // Konversi nilai adc dari 8 bit adc menjadi 10 bit adc
	value = map(value, 0, 255, 0, 1023);
	
    // Apabila nilai adc antara 280-512 (apabila dikonversi cm menjadi 550cm sampai 100cm)
    if (value >= 280 && value <= 512)
    return 28250/(value-229.5);     // Hitung nilai jarak 
    
    // Apabila nilai adc diluar batas
    else return OUT_OF_RANGE;       // Diluar batas

}



// ----------------------------------------------------------------------------------------
// Melakukan banyak pengukuran kemudian mengurutkan dan mengambil nilai median
// ----------------------------------------------------------------------------------------
//      Misal dalam 5x pengukuran mendapatkan hasil 100, 330, 220, 150, 225
//      Setelah data dirutkan menjadi 100, 150, 220, 225, 330
//      Setelah diambil nilai tengah/median yaitu 220 cm
// ----------------------------------------------------------------------------------------
uint16_t read_median(uint8_t it) {
	uint16_t dist[it], last;
	uint8_t j, i = 0;
	dist[0] = OUT_OF_RANGE;

	while (i < it) {
		last = read_distance_cm();  

		if (last != OUT_OF_RANGE) {      
			if (i > 0) {            
				for (j = i; j > 0 && dist[j - 1] < last; j--){
					dist[j] = dist[j - 1];                     
				}
			} else {
				j = 0;
			}       

			dist[j] = last;              
			i++;                       
		} else {
			it--;                   
		} 
	}
	return (dist[it >> 1]); 
}
