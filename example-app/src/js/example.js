import { TruvideoSdkMedia } from 'truvideo-capacitor-media-sdk';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    TruvideoSdkMedia.echo({ value: inputValue })
    
}
