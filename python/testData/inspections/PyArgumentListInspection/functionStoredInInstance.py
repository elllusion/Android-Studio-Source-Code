def xlistdir(path): return path

class Directory:
    listdir = xlistdir
    #...
    def listing(self):
    #....
        self.listdir("path")  #<---here
    #...