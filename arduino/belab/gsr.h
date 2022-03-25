#include <sensor.h>

class gsr: public sensor{
private:
    /* data */
    int sensorValue;
    char INPUTPIN[2];
public:
    gsr(char* PIN);
    ~gsr();

    void getValue();
    bool isConnect();
    long sensor2resistance();
};

gsr::gsr(char* PIN)
{
    for (int i = 0; i < 2; i++) INPUTPIN[i] = PIN[i];
}

gsr::~gsr()
{
}

void gsr::getValue(){
    sensorValue = analogRead(INPUTPIN);
}

bool gsr::isConnect(){
    bool condition;
    return condition;
}

long gsr::sensor2resistance(){
    long resistance = ((1024 + 2 * sensorValue) * 10000) / (512 - sensorValue);
    return resistance;
}