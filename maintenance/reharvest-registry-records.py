"""
A script that re-harvests vg:Registry-typed records known to the
VO Registry from themselves.

This will not work when they change their access URLs in "one step"
(i.e., without letting us harvest them at their old address).
"""

import pathlib
import sys
from xml.etree import ElementTree

import requests
import pyvo

RECORDS_DIR = pathlib.Path("registry-records")
RECORDS_DIR.mkdir(exist_ok=True)
RI_NS = "http://www.ivoa.net/xml/RegistryInterface/v1.0"

def get_registries_regtap():
    """returns (ivoid, access_url) for the registries from RegTAP.
    """
    svc = pyvo.dal.TAPService("http://dc.g-vo.org/tap")
    return [(ivoid, url) for ivoid, url in svc.run_sync(
        """SELECT ivoid, access_url
           FROM rr.capability
           NATURAL JOIN rr.interface
           WHERE standard_id='ivo://ivoa.net/std/registry'
             AND cap_type='vg:harvest'""").to_table()]


def get_registries_rofr():
    """Dustin: that's something you need to write
    """


def gen_file_name(ivoid):
    """returns a file name for the registry record for ivoid.

    Dustin: this would probably have to be adopted to what you currently do.
    """
    return ivoid.split("/")[2]+".xml"


def reharvest(registries):
    """blindly re-pulls the record for the publishing registry from itself.

    It *might* be referable to first see whether it has been updated
    using ListIdentifiers and a from clause, perhaps based on the file
    date.  But then we are talking about almost trivial amounts of data,
    and so I suppose just blindly pulling the records is just fine and
    saves implementation work.
    """
    for ivoid, access_url in registries:
        try:
            oai_reply = requests.get(access_url,
                # heasarc takes forever, so give it time
                timeout=50,
                params={
                    "verb": "GetRecord",
                    "metadataPrefix": "ivo_vor",
                    "identifier": ivoid,})
            tree = ElementTree.fromstring(oai_reply.text)
            resource = tree.find(f".//{{{RI_NS}}}Resource")
            with open(RECORDS_DIR / gen_file_name(ivoid), "wb") as f:
                ElementTree.ElementTree(resource).write(f, encoding="utf-8")
        except KeyboardInterrupt:
            raise
        except Exception as msg:
            sys.stderr.write(
                f"Failed to re-harvest {access_url}?verb=GetRecord&"
                f"metadataPrefix=ivo_vor&identifier={ivoid}"
                f": {msg}\n")


def main():
    reharvest(get_registries_regtap())


if __name__=="__main__":
    main()

# vim:et:sta:sw=4
