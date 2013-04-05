package com.neophob.udp2i2c.udp;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import com.neophob.udp2i2c.i2c.I2cHelper;
import com.neophob.udp2i2c.model.I2cConfig;

public class UdpServerHandler extends SimpleChannelUpstreamHandler {

	private final I2cConfig i2cConfig;
	
	public UdpServerHandler(I2cConfig i2cConfig) {
		this.i2cConfig = i2cConfig;
	}

	/**
	 * callback if one packet arrives. it contains data for one panel
	 */
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		ChannelBuffer msg = (ChannelBuffer) e.getMessage();
		
		//read tpm2.net header
		byte tpm2netStart = msg.getByte(0);
		byte tpm2netCmd = msg.getByte(1);
		
		//check if header is valid
		if (tpm2netStart!=(byte)0x9c || tpm2netCmd!=(byte)0xda) {
			System.out.println("Error, invalid header data recieved! "+String.valueOf(tpm2netStart&255));
			return;
		}
		
		short frameSize = msg.getShort(2);
		byte nr = msg.getByte(4);
				
		int nrOfDevices = i2cConfig.getI2cAddress().size();
		//subtract 6 bytes, 5 bytes header and 1 byte footer
		int length = (msg.array().length-6) / nrOfDevices;
		
		System.out.println("framesize: "+frameSize+", packet number: "+nr+", data per panel: "+length);
		
		int srcPos = 0;
		byte[] buffer = new byte[length];
		for (int id: i2cConfig.getI2cAddress()) {
			System.arraycopy(msg.array(), srcPos, buffer, 0, length);
			
			try {
				I2cHelper.sendData(i2cConfig.getI2cBus(), id, buffer);							
			} catch (Exception ex) {
				System.out.println("failed to send data to i2c address "+id);
				ex.printStackTrace();
			}
		}
	}
	
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
    	System.out.println("Ooops, error detected!");
    	e.getCause().printStackTrace();
        // We don't close the channel because we can keep serving requests.
    }

}