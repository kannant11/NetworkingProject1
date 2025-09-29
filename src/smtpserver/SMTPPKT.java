package smtpserver;

import maildirsupport.MailMessage;

public class SMTPPKT {
    
    private enum CommandType {HELO, MAIL, RCPT, DATA, RSET, NOOP, QUIT}
    String val;
    CommandType commandType;

    SMTPPKT(MailMessage msg){

        val = msg.getMessage();
        commandType = CommandType.valueOf(msg.getMessage().substring(0, 4).toUpperCase());

    }

    public CommandType getCommandType(){

        return commandType;

    }

    public String getVal(){

        return val;

    }

    public void Protocol(CommandType type){

        switch(type){

            case HELO:
                // handle HELO command

                

                break;

            case MAIL:
                // handle MAIL command
                break;

            case RCPT:
                // handle RCPT command
                break;

            case DATA:
                // handle DATA command
                break;

            case RSET:
                // handle RSET command
                break;

            case NOOP:
                // handle NOOP command
                break;

            case QUIT:
                // handle QUIT command
                break;

        }

    }

}


