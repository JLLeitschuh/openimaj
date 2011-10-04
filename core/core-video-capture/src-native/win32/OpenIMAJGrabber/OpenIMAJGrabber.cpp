#include <iostream>

#include "OpenIMAJGrabber.h"
#include <vector>
#include <string.h>
#include <sstream>
#include <stdlib.h>
using namespace std;

void error(const char *str) {
    fprintf(stderr, "%s", str);
}

#define DEBUGOUT 1



/** DEVICE + DEVICELIST **/

Device::Device(const char* name, const char* identifier) {
    this->name = new char[strlen(name) + 1];
    this->identifier = new char[strlen(identifier) + 1];
    
    strcpy((char*)this->name, name);
    strcpy((char*)this->identifier, identifier);
}

Device::~Device() {
    delete [] name;
    delete [] identifier;
}

DeviceList::DeviceList(Device** devices, int nDevices) {
    this->nDevices = nDevices;
    this->devices = devices;
}

DeviceList::~DeviceList() {
    delete [] devices;
}

int DeviceList::getNumDevices() {
    return nDevices;
}

Device * DeviceList::getDevice(int i) {
    return devices[i];
}

const char* Device::getName() {
    return name;
}

const char* Device::getIdentifier() {
    return identifier;
}

/** END DEVICE + DEVICE LIST**/

OpenIMAJGrabber::OpenIMAJGrabber(){
	// cout << "Called this constructor ey?" << endl;
	this->data = (void*)new videoData();
	((videoData*)this->data)->VI = new videoInput();
	((videoData*)this->data)->device = 0;
	((videoData*)this->data)->buffer = NULL;
}
OpenIMAJGrabber::~OpenIMAJGrabber(){
	if(((videoData*)this->data)->buffer!=NULL)
		delete[] ((videoData*)this->data)->buffer;
	delete ((videoData*)this->data)->VI;
	delete ((videoData*)this->data);

}

DeviceList * OpenIMAJGrabber::getVideoDevices(){
	int count = ((videoData*)this->data)->VI->listDevices();
	Device ** devices = new Device*[count];
    
    for (int i=0; i<count; i++) {
		const char * name = ((videoData*)this->data)->VI->getDeviceName(i);
        std::stringstream ss;//create a stringstream
		ss << i;//add number to the stream
        devices[i] = new Device(name,ss.str().c_str());
    }
        
    return new DeviceList(devices, count);
}

bool OpenIMAJGrabber::startSession(int width, int height,double rate)
{
	if(this->getVideoDevices()->getNumDevices() <= ((videoData*)this->data)->device)
		return false;
	return this->startSession(
		width,height,rate,
		this->getVideoDevices()->getDevice(((videoData*)this->data)->device)
	);
}

bool OpenIMAJGrabber::startSession(int width, int height,double rate, Device* dev){
	if(((videoData*)this->data)->buffer != NULL){
		delete [] ((videoData*)this->data)->buffer;
		((videoData*)this->data)->VI->stopDevice(((videoData*)this->data)->device);
		((videoData*)this->data)->buffer = NULL;
	}
	// cout << "Checked the buffer, creating new buffer" << endl;
	((videoData*)this->data)->device = atoi(dev->getIdentifier());

	if(this->getVideoDevices()->getNumDevices() <= ((videoData*)this->data)->device)
		return false;
	//cout << "Current device is: " << ((videoData*)this->data)->device << endl;
	((videoData*)this->data)->VI->setupDevice(((videoData*)this->data)->device,width,height);
	//cout << "Device set up, initialising" << endl;

	if(((videoData*)this->data)->VI->isDeviceSetup(((videoData*)this->data)->device)){
		((videoData*)this->data)->width = ((videoData*)this->data)->VI->getWidth(((videoData*)this->data)->device);
		//cout << "Getting width: " << ((videoData*)this->data)->width << endl;
		((videoData*)this->data)->height = ((videoData*)this->data)->VI->getHeight(((videoData*)this->data)->device);
		//cout << "Getting height: " << ((videoData*)this->data)->height << endl;
		((videoData*)this->data)->size	= ((videoData*)this->data)->VI->getSize(((videoData*)this->data)->device);
		((videoData*)this->data)->buffer = new unsigned char[((videoData*)this->data)->size];
		//cout << "Buffer created!"<< endl;
	}

	return ((videoData*)this->data)->VI->isDeviceSetup(((videoData*)this->data)->device);
}

void OpenIMAJGrabber::stopSession(){
	if(((videoData*)this->data)->buffer != NULL){
		delete [] ((videoData*)this->data)->buffer;
		((videoData*)this->data)->buffer = NULL;
	}
	if(((videoData*)this->data)->VI->isDeviceSetup(((videoData*)this->data)->device))
	{
		((videoData*)this->data)->VI->stopDevice(((videoData*)this->data)->device);
	}
}

unsigned char* OpenIMAJGrabber::getImage(){
	return ((videoData*)this->data)->buffer;
}
void OpenIMAJGrabber::nextFrame(){
	if(((videoData*)this->data)->VI->isFrameNew(((videoData*)this->data)->device)){
		((videoData*)this->data)->VI->getPixels(((videoData*)this->data)->device, ((videoData*)this->data)->buffer, true, true);	//fills pixels as a BGR (for openCV) unsigned char array - no flipping
	}
}
        
int OpenIMAJGrabber::getWidth(){
	return ((videoData*)this->data)->width;
}
int OpenIMAJGrabber::getHeight(){
	return ((videoData*)this->data)->height;
}


/*
int main(char** argv, int argc){
	OpenIMAJGrabber * grabber = new OpenIMAJGrabber();
	grabber->startSession(100,100);
	cout << grabber->getWidth() << "," << grabber->getHeight() << endl;
	while(1){grabber->nextFrame();}


	//create a videoInput object
	videoInput VI;
	
	//Prints out a list of available devices and returns num of devices found
	int numDevices = VI.listDevices();	
	
	int device1 = 0;  //this could be any deviceID that shows up in listDevices
	
	//if you want to capture at a different frame rate (default is 30) 
	//specify it here, you are not guaranteed to get this fps though.
	//VI.setIdealFramerate(dev, 60);	
	
	//setup the first device - there are a number of options:
	
	VI.setupDevice(device1); 						  //setup the first device with the default settings
	//VI.setupDevice(device1, VI_COMPOSITE); 			  //or setup device with specific connection type
	//VI.setupDevice(device1, 320, 240);				  //or setup device with specified video size
	//VI.setupDevice(device1, 320, 240, VI_COMPOSITE);  //or setup device with video size and connection type

	//VI.setFormat(device1, VI_NTSC_M);					//if your card doesn't remember what format it should be
														//call this with the appropriate format listed above
														//NOTE: must be called after setupDevice!
	
				  

	//As requested width and height can not always be accomodated
	//make sure to check the size once the device is setup

	int width 	= VI.getWidth(device1);
	int height 	= VI.getHeight(device1);
	int size	= VI.getSize(device1);
	
	unsigned char * yourBuffer1 = new unsigned char[size];
	
	//to get the data from the device first check if the data is new
	if(VI.isFrameNew(device1)){
		VI.getPixels(device1, yourBuffer1, false, false);	//fills pixels as a BGR (for openCV) unsigned char array - no flipping
	}
	
	//same applies to device2 etc
	
	//to get a settings dialog for the device
	VI.showSettingsWindow(device1);
	
	
	//Shut down devices properly
	VI.stopDevice(device1);
}
*/
