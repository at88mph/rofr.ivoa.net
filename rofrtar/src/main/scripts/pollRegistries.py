#! /usr/bin/env python
#

# The uiuc site had the psi package installed.  The install here is TBD.
# psi sources are at https://pypi.python.org/pypi/PSI/0.3b2

import sys
sys.path.append("/export/pnh/Python/lib64/python2.6/site-packages")

import sys, os, re, shutil
import zlib, logging.handlers
import subprocess as sub
from optparse import OptionParser

prog = os.path.basename(sys.argv[0])
if not prog:
    prog = "pollRegistries"
bindir = os.path.dirname(sys.argv[0])
if not bindir:
    bindir = sys.path[0]
if os.environ.has_key('ROFR_HOME'):
    sysdir = os.environ['ROFR_HOME']
else:
    sysdir = os.path.join(bindir, "..")

vardir = os.path.join(sysdir, "var", "pollRegistries")
registryListFile = os.path.join(vardir, "searchable.lis")
listRegBin = os.path.join(sysdir, "bin", "listregistries")
xsltprocBin = "xsltproc"
extracter = os.path.join(sysdir, "lib", "makelookup.xsl")
pidfile = os.path.join(vardir, prog + ".pid")

usage = "Usage: %prog [-h] [-q] [-l logfile]"

def main(args=sys.argv[1:]):
    cli = getCLIParser()
    (cli.opts, cli.args) = cli.parse_args(args)
    if not cli.opts.logfile:
        cli.opts.logfile = os.path.join(vardir, prog + ".log")

    if not checkPID():
        if not cli.opts.quiet:
            print >> sys.stderr, "%s: already running; exiting!" % prog
        return 0

    log = getLogger(cli.opts.logfile)
    log.info("+++ polling begins")

    regs = getRegistryLookup(registryListFile)
    count = {}

    for reg in regs['_order']:
        datadir = os.path.join(vardir, dataDirName(reg))
        if not os.path.exists(datadir):
            os.makedirs(datadir)
        reglistfile = os.path.join(datadir, "registries.xml")
        try:
            searchForRegistries(regs[reg], reglistfile)
            count[reg] = ""
        except:
            log.info("Problem accessing " + reg)
            count[reg] = "-"
            continue

        lookupfile = os.path.join(datadir, "searchable.lis")
        try:
            count[reg] = extractSearchables(reglistfile, lookupfile)
        except:
            log.info("Problem identifying searchable registries from " + reg)
            count[reg] = "-"

    # now choose the registry for the sourse of our new global searchable list.
    okay = filter(lambda id: count.has_key(id) and isinstance(count[id], int),
                  count.keys())
    if not okay:
        # don't update our list of searchable registries
        log.warn("No good search results found")
        log.info("+++ polling done")
	if os.path.exists(pidfile):
	    os.remove(pidfile)
        return 0
        
    maxcount = max(map(lambda id: count[id], okay))
    okay = filter(lambda id: count[id] == maxcount, okay)
    if not okay:
        # don't update our list of searchable registries
        log.warn("No good search results found")
        log.info("+++ polling done")
	if os.path.exists(pidfile):
	    os.remove(pidfile)
        return 0

    use = None
    for id in regs['_order']:
        if id in okay:
            use = id
            break
    if not use:
        use = okay[0]

    srcfile = os.path.join(vardir, dataDirName(use), "searchable.lis")
    srcregs = getRegistryLookup(srcfile)

    # now create the new searchable list:
    out = { '_order': [] }
    for id in regs['_order']:
        nid = id
        if count[id] == '-':
            nid = count[id]+id
        out['_order'].append(nid)
        if srcregs.has_key(id):
            out[nid] = srcregs[id]
        else:
            out[nid] = regs[id]
    for id in srcregs['_order']:
        if id not in regs.keys():
            out['_order'].append(id)
            out[id] = srcregs[id]

    # write it out 
    fd = open(registryListFile, 'w')
    try:
        for id in out['_order']:
            print >> fd, id, out[id]
    finally:
        fd.close()

    reglist = os.path.join(vardir, dataDirName(use), "registries.xml")
    shutil.copyfile(reglist, os.path.join(vardir, "registries.xml"))
    log.info("+++ polling done")

    if os.path.exists(pidfile):
        os.remove(pidfile)
    return 0

def getCLIParser():
    out = OptionParser(usage=usage)
    out.add_option('-l', '--log', action='store', dest="logfile",
                   help="record status information to the named log file")
    out.add_option('-q', '--quiet', action='store_true', dest="quiet", 
                   default=False,
                   help="Don't report that another instance of this script is running")
    return out

def getLogger(logfile=None):
    log = logging.getLogger()
    log.setLevel(logging.INFO)
    hdlr = logging.StreamHandler()
    hdlr.setLevel(logging.WARN)
    fmtr = logging.Formatter(prog + ": %(message)s")
    hdlr.setFormatter(fmtr)
    log.addHandler(hdlr)

    if logfile:
        hdlr = logging.handlers.TimedRotatingFileHandler(logfile, when='W3',
                                                         interval=4)
        hdlr.setLevel(logging.INFO)
        fmtr = logging.Formatter("%(asctime)s %(levelname)s: %(message)s")
        hdlr.setFormatter(fmtr)
        log.addHandler(hdlr)

    return log

def getRegistryLookup(file):
    out = {}
    ids = []
    fd = open(file)
    try:
        for line in fd:
            line = line.strip()
            try:
                id, accessurl = line.split()[:2]
            except ValueError, ex:
                print >> sys.stderr, "Trouble parsing registry lookup list:", \
                    line

            okay = id[0]
            if okay == '+' or okay == '-':
                id = id[1:]
            else:
                okay = '+'

            ids.append(id)
            out[id] = accessurl

    finally:
        fd.close()

    out["_order"] = ids
    return out

idre = re.compile(r'\w+://([^/]+)')
def dataDirName(id):
    match = idre.search(id)
    if not match:
        raise ValueError("Bad registry id: " + id)

    return "%s%d" % (match.group(1), zlib.crc32(id))

def extractSearchables(infile, outfile):
    dataDir = os.path.dirname(outfile)
    if not os.path.exists(dataDir):
        os.makedirs(dataDir)

    # run the query 
    errlog = os.path.join(dataDir, "error.log")
    err = open(errlog, 'a')
    cmd = "%s -o %s %s %s" % (xsltprocBin, outfile, extracter, infile)
    try:
        subp = sub.Popen(cmd.split(), bufsize=1, stderr=err)
    except Exception, e:
        print >> err, "xsltproc:", str(e)
        err.close()
        raise

    subp.wait()
    err.close()

    msg = None
    if subp.returncode != 0:
        err = open(errlog)
        try:
            msg = err.readline().strip()
        finally:
            err.close()

    err = open(errlog,'a')
    print >> err, listRegBin, "exits with code=%d" % subp.returncode

    if msg:
        err.close()
        raise RuntimeError("Extraction failed: " + msg)

    subp = sub.Popen(("wc -l " + outfile).split(), 
                     bufsize=1, stdout=sub.PIPE, stderr=err)
    err.close()
    if subp.wait() != 0:
        raise RuntimeError("resource count failed")
    count = subp.stdout.read().strip().split()[0]
    if not count:
        raise RuntimeError("Failed to parse resource count")

    return int(count)

def searchForRegistries(accessurl, outfile):
    dataDir = os.path.dirname(outfile)
    if not os.path.exists(dataDir):
        os.makedirs(dataDir)

    # run the query 
    errlog = os.path.join(dataDir, "error.log")
    err = open(errlog, 'w')
    try: 
        query = sub.Popen(("%s %s" % (listRegBin, accessurl)).split(), 
                          bufsize=1, stdout=sub.PIPE, stderr=err)
    except Exception, e:
        print >> err, "listregistries:", str(e)
        err.close()
        raise

    out = open(outfile, 'w')

    try:
        depth = 0
        step = 2
        for sline in query.stdout:
            sline = sline.rstrip()
            lines = ">\n<".join(sline.split('><')).split('\n')
            for line in lines:
                if line[0] == '<':
                    if line[1] == '/':
                        depth -= 1
                        if depth < 0: depth = 0
                        out.write((step*depth) * ' ')
                    else:
                        out.write((step*depth) * ' ')
                        if line[1] != '?':
                            depth += 1
                    lim = len(line)-1
                    for i in xrange(1, lim+1):
                        if line[i] == '<':
                            if i < lim and line[i+1] == '/':
                                depth -= 1
                            else:
                                depth += 1
                        elif line[i] == '>':
                            if line[i-1] == '/':
                                depth -= 1
                print >> out, line
    finally:
        out.close()
        query.wait()
        err.close()

        msg = None
        if query.returncode != 0:
            err = open(errlog)
            try:
                msg = err.readline().strip()
            finally:
                err.close()
        err = open(errlog,'a')
        print >> err, listRegBin, "exits with code=%d" % query.returncode
        err.close()

        if msg:
            raise RuntimeError("Query failed: " + msg)

def checkPID():
    import fcntl
    if os.path.exists(pidfile):
        import psi
        import psi.process
        pidf = open(pidfile, 'r')
        try:
            fcntl.flock(pidf, (fcntl.LOCK_EX|fcntl.LOCK_NB))
        except IOError:
            pidf.close()
            return False
        try:
            (progname, pid) = pidf.read().strip().split()
        except ValueError:
            pidf.close()
            print >> sys.stderr, "Bad PID file contents:", pidfile
            return False
        pidf.close()

        try:
            proc = psi.process.Process(int(pid))
            pidprog = os.path.basename(proc.exe)
            if pidprog == "python":
                argv = proc.args[1:]
                while len(argv) > 0 and argv[0].startswith('-'):
                    argv.pop(0)
                if len(argv) > 0:
                    pidprog = os.path.basename(argv[0])
            if progname == pidprog:
                return False
        except ValueError:
            pass

    pidf = open(pidfile, "w")
    try:
        fcntl.flock(pidf.fileno(), (fcntl.LOCK_EX|fcntl.LOCK_NB))
    except IOError:
        pidf.close()
        return False
    print >> pidf, prog, os.getpid()
    pidf.close()
    return True


if __name__ == '__main__':
    sys.exit(main())
