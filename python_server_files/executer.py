import csv

class Executer:
    def __init__(self, tcpServer):
        self.andRaspTCP = tcpServer

    def fileread(self, path):
        string = 'C:\\Users\\kitte\\Desktop\\Data\\' + str(path) + '.csv'
        row = []
        with open(string, 'r') as f:
            rdr = csv.reader(f)
            for r in rdr:
                row.append(r)
        line = ''
        for l in row:
            line = line + str(l)
        return line

    def startCommand(self, command):
        if(command[0]=='1'):
            r=str(self.fileread(command[2:-2]))
            self.andRaspTCP.sendAll(r + "\n")
        if(command[0]=='2'):
            r1=str(self.fileread(command[2:9]))
            r2=str(self.fileread(command[10:-2]))
            self.andRaspTCP.sendAll(r1 + ">" + r2 + "\n")
        if(command[0]=='3'):
            r1=str(self.fileread(command[2:9]))
            r2=str(self.fileread(command[10:17]))
            r3=str(self.fileread(command[18:-2]))
            self.andRaspTCP.sendAll(r1 + ">" + r2 + ">" + r3 + "\n")
