#include "def.h"
#include <SoftwareSerial.h>

SoftwareSerial BT(PIN_Rx, PIN_Tx);

/*
int sensorValue=0;
long resistance_average=0;
*/

int sensorValues[5];
char msg;

void setup(){
    // Serial
    Serial.begin(9600);
    
    // PIN Configuration
    pinMode(PIN_Rx, INPUT);
    pinMode(PIN_Tx, OUTPUT);
    
    // Bluetooth setting
    BT.begin(9600);
}

void loop(){
    sensorValues[0] = analogRead(A1);
    Serial.println(sensorValues[0]);
    delay(5);
}
