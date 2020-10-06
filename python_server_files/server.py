import tcpServer
import executer
from multiprocessing import Queue
#실행해야 하는 파일

commandQueue = Queue()

#                                              아이피 변경하기*
andRaspTCP = tcpServer.TCPServer(commandQueue, "172.30.1.51", 7777)
andRaspTCP.start()

commandExecuter = executer.Executer(andRaspTCP)

while True:
    try:
        command = commandQueue.get()
        commandExecuter.startCommand(command)
    except:
        pass