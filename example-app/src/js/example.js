import { MediaPlugin } from 'truvideo-capacitor-media-sdk';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    MediaPlugin.echo({ value: inputValue })
}
