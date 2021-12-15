import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerInputHandler extends ChannelInboundHandlerAdapter {

    static final List<ClientChannel> channels = new ArrayList<ClientChannel>();
    private String clientNickname = "";
    boolean isReadComplete = false;
    byte[] byteArray;

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        System.out.println("Client joined - " + ctx);
        channels.add(new ClientChannel(ctx.channel()));
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ExchangeFormat clientRequest = (ExchangeFormat) msg;

        if (clientRequest.getParcelType() == Tool.RequestType.GREETING) {
            acceptNickname(clientRequest.getUsername());
            return;
        }

        if (clientRequest.getParcelType() == Tool.RequestType.EXIT) {
            channels.remove(getCurrentClientChannel());
            notifyAboutUserExit(getCurrentClientNickname());
            ctx.close();
            return;
        }

        processDefaultMessage(clientRequest);
    }


    private void acceptNickname(String desiredNickname) {
        ExchangeFormat responseException = new ExchangeFormat();

        for (ClientChannel c : channels) {
            if (c.getNickname().equals(desiredNickname)) {
                responseException.setParcelType(Tool.RequestType.EXCEPTION);
                responseException.setMessage("1");
                responseException.setTime(Tool.getCurrentTime());
                getCurrentClientChannel().getChannel()
                        .writeAndFlush(getByteBufParcel(responseException));
                return;
            }
        }
        channels.get(channels.size() - 1).setNickname(desiredNickname); // last client == current client
        clientNickname = desiredNickname;

        ExchangeFormat response = new ExchangeFormat();
        response.setParcelType(Tool.RequestType.GREETING);
        response.setUsername(desiredNickname);
        response.setTime(Tool.getCurrentTime());

        broadcastMessage(response);

    }

    private void processDefaultMessage(ExchangeFormat clientRequest) {
        ExchangeFormat serverResponse = new ExchangeFormat();

        serverResponse.setParcelType(Tool.RequestType.MESSAGE);
        serverResponse.setTime(Tool.getCurrentTime());
        serverResponse.setUsername(getCurrentClientNickname());
        serverResponse.setMessage(clientRequest.getMessage());

        if (clientRequest.getAttachmentSize() != 0) {
            byteArray = clientRequest.getAttachmentByteArray();


            System.out.println("клиент вложил файл");
            serverResponse.setAttachmentName(clientRequest.getAttachmentName());
            serverResponse.setAttachmentSize(clientRequest.getAttachmentSize());
            serverResponse.setAttachmentByteArray(byteArray);
            broadcastMessageWithFile(serverResponse);
            return;
        }

        broadcastMessage(serverResponse);

    }

    private ClientChannel getCurrentClientChannel() {
        for (ClientChannel c : channels) {
            if (c.getNickname().equals(clientNickname)) return c;
        }
        return channels.get(channels.size() - 1);
    }

    private String getCurrentClientNickname() {
        return getCurrentClientChannel().getNickname();
    }

    private ByteBuf getByteBufParcel(ExchangeFormat response) {

        return Unpooled.copiedBuffer(response.toParcel(), Charset.defaultCharset());
    }

    private void notifyAboutUserExit(String clientNickname) {
        ExchangeFormat notifyParcel = new ExchangeFormat();
        notifyParcel.setParcelType(Tool.RequestType.EXIT);
        notifyParcel.setUsername(clientNickname);
        notifyParcel.setTime(Tool.getCurrentTime());
        broadcastMessage(notifyParcel);
    }

    private void broadcastMessage(ExchangeFormat response) {
        for (ClientChannel c : channels) {
            c.getChannel().write(getByteBufParcel(response));
            c.getChannel().flush();
        }
    }

    private void broadcastMessageWithFile(ExchangeFormat response) {
        Channel channel;
        for (ClientChannel c : channels) {
            channel = c.getChannel();
            if (c.getNickname().equals(clientNickname)) {
                channel.write(getByteBufParcel(response));
                channel.flush();
            } else {
                channel.write(getByteBufParcel(response));
                channel.write(Unpooled.copiedBuffer(response.getAttachmentByteArray()));
                channel.flush();
            }
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        channels.remove(getCurrentClientChannel());
        notifyAboutUserExit(clientNickname);
        ctx.close();
    }
}
