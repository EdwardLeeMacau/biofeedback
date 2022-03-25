#///////////////////////////////////////////////////////////////////////////////
# Files   function.py: all function and data is defined
        #      -> raw_data.py: 
        #            >> define the sensors in SensorsClass()
        #            >> getdata() and parsedata()
        #      -> therapysteps.py: 
        #            >> define the pages in PageClass()
        #            >> calculate the data and determine whether to enter next page
        #      -> send.py:
        #            >> define the wifi
# Variables 
        # _output               -> the output path of csv file
        # _user_defined_steps   -> if not empty, then the main() would follow the step
#///////////////////////////////////////////////////////////////////////////////
import argparse
import lib
import sys
import csv
# flags -----------------------------------------------------------------
_verbose = False
_debug = False

parser = argparse.ArgumentParser(description="BELab Final Project")
parser.add_argument("-v", "--verbose", action="store_true", help="Increase output verbosity")
parser.add_argument("-d", "--debug", action="store_true", help="Start debug mode.")
args = parser.parse_args()
# variables -----------------------------------------------------------------
_output = "output1"
_user_defined_steps = [1,2,0,1,2]

# initialization -----------------------------------------------------------------
_myfunc = lib.function.Function(_user_defined_steps)


if args.verbose:
    _verbose = True
    print("Verbose Mode Open")

if args.debug:
    _debug = True
    print("Debug Mode Open")

def main():    
    if ( 1!=len(sys.argv)):
        sys.exit("Expect only one argument")
# Check that every sensor is functional, if not, exit()-----------
    for x in _myfunc._mySensor:
        if(not x.initSensor()):
            sys.exit(str(x.name)+ " is a Bad Sensor")
# Check that Wifi is function, if not, exit()---------------------    
    if(not lib.send.initWifi()):
        sys.exit("Bad Wifi")
# initialize csvfile writer --------------------------------------
    _sensorName = list()
    try:
        fw = open (_output,'w',newline = '\n') 
        csv_w = csv.writer(fw)
        # write the keys into csv_w -----------------------------
        for x in _myfunc._mySensor: 
            _sensorName.append (x.name)
        csv_w.writerow(_sensorName)
        # write the keys into csv_w -----------------------------
    except IOError:
        sys.exit ("File not found!!")

# set the first page index ---------------------------------------
    if(_user_defined_steps != []):
        _currentPageIdx = _user_defined_steps.pop(0)
    else: _currentPageIdx = 0
########################### main body #########################
    while(_currentPageIdx >=0):
        _myfunc.executeSensor(csv_w)
        _currentPageIdx = _myfunc.executePage(_currentPageIdx)
        _myfunc.executeWifi (_currentPageIdx)
########################### main body #########################
    sys.exit("----------------Process Complete!!------------------")


if __name__ == "__main__":
    main()
