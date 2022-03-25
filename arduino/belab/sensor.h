
class sensor
{
private:
    /* data */
    int sensorValue;
public:
    sensor(/* args */);
    ~sensor();

    virtual bool isConnect();
    virtual void getValue();
};

sensor::sensor(/* args */)
{
}

sensor::~sensor()
{
}