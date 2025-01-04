package com.valiantgaming.commons.network.packet;

public interface Protocol
{
    /*
        ------------ KEY ------------
                A = AuthServer
                B = BattleServer
                C = ChatServer
                D = DatabaseServer
                F = FieldServer
                G = GameServer
                L = Launcher
                S = ShopServer
                U = User
                W = WebServer
        -----------------------------
     */

    /*
        ====================================
                  SERVER TO CLIENT
        ====================================
     */
    // ------------ AuthServer ------------
    byte A2U_ansReady       = 0x00;
    byte U2A_askVerify      = 0x01;
    byte A2U_ansVerify      = 0x02;
    byte U2A_askAuthUser    = 0x03;
    /*
        C2S_askAuthUser PACKET HAS CHANGED!

        >> Packet Size: 5d 00 (LE. Otherwise, 00 5d)
        >> Category: 33
        >> Protocol: 03
        >> UID Size?: 08
        >> Filler: 00 00 00
        >> UID?: 69 cd 82 02 02 a1 0f 00
                NOTE: Provided UID in decimal: 42126697. Hex: 02 82 CD 69.
                So the first 4 bytes only match in LE.
        >> Filler: 00
        >> Username: 41 69 6f 48 61 72 75 6b 61 00
                     00 00 00 00 00 00 00 00 00 00
                     00 00 00 00 00 00 00 00 00 00
                     00 00 00 00 00 00 00 00 00 00
        >> Filler: 00
        >> Encrypted Password: 39 34 6b 38 38 32 64 32 64 63
                               39 39 68 39 63 64 36 39 64 78
                               62 32 68 62 32 6b 6b 32 37 31
                               30 33 61 66 62 37
                NOTE: Whatever is passed to the client.
        >> Filler: 00
     */
    byte A2U_ansAuthUser    = 0x0E;
    byte U2A_askSrvList     = 0x0F;
    byte A2U_ansSrvList_Srv = 0x11;
    byte A2U_ansSrvList_Chn = 0x12;
    byte U2A_askSrvSelect   = 0x13;
    byte A2U_ansSrvSelect   = 0x1A;

    /*
    WEBZEN WEB PLUGIN PASSES COOKIE DATA TO LAUNCHER PRIOR TO PACKET TRANSFER.
    Inside the client Log files... see [Client]\Log
        ID: 42126697
        Username: AioHaruka
        Encrypted Password: f3 82 86 9f cd 69 b9 65 dc bc 08 cf 02 20 4a 87 c1 cc
        Unknown: 2|1|1|2|2
        >> NOTE: If you fed this data to sungame.exe then this "f3 82 86 9f cd 69 b9 65 dc bc 08 cf 02 20 4a 87 c1 cc"
        will show up in the verify login packet.

    Web-check for update client version through packet.
        >> EXAMPLE: GET /classic/sun/service/update.uvf HTTP/1.1\r\n

    Web-check for update launcher version through packet.
        >> EXAMPLE: GET /launcher HTTP/1.1\r\n

    LAUNCHER:
        C2S_askUnknown1: 0xFE - NO DATA
        S2C_ansReady (Tea Key)
        S2C_ansVerifyVersion = 0xFF
            >> Example:
                Packet Size: 0e 00
                Category: 33
                Protocol: ff
                Data: 01 00 09 02 02 04 00 03 00 06 00 00
                    >> Launcher Version: 01 00 09 02
                    >> Client Version: 02 04 00 03
                    >> Unknown: 00 06 00 00

     FROM HERE THE CLIENT DOES ALL NORMAL STEPS!
     */

    /*
        ====================================
                  SERVER TO SERVER
        ====================================
     */
    // ---------- Common Packets ----------
    byte S2S_askAesFileKey = 0x00;
    byte S2S_ansAesFileKey = 0x01;
    byte S2S_askRsaKey = 0x02; // ABLE TO DECRYPT FILE AND OBTAIN SHA SYSTEM KEY.
    byte S2S_ansRsaKey = 0x03; // MAC GENERATED AND ATTACHED TO PACKET.
    byte S2S_askAesKey = 0x04;
    byte S2S_ansAesKey = 0x05; // PACKET ENCRYPTION ENABLED!
    byte S2S_askServerInfo = 0x06;
    byte S2S_ansServerInfo = 0x07;
}