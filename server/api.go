package muzeighibli

import (
    "io"
    "fmt"
	"bytes"
    "time"
    "sort"
    "bufio"
    "math"
    "math/big"
    "reflect"
    "strings"
    "net/http"
    "image"
    "image/png"
    "image/jpeg"
    "unicode"
    "encoding/json"
    "appengine"
    "appengine/urlfetch"
    "appengine/datastore"
    "appengine/taskqueue"
    "appengine/memcache"
    gwc "github.com/jyotiska/go-webcolors"
    "github.com/nfnt/resize"
    "github.com/tv42/base58"
)

type Artwork struct {
    Id            int64         `json:"-" datastore:"-"`
    Aid           string        `json:"aid"`
    Hash          string        `json:"hash"`
    Url           string        `json:"url"`
    UrlSmall      string        `json:"url_small"`
    Width         int           `json:"width"`
    Height        int           `json:"height"`
    Ratio         float32       `json:"ratio"`
    Caption       string        `json:"caption"`
    Subtitle      string        `json:"subtitle"`
    Hidden        bool          `json:"-"`
    Free          bool          `json:"-"`

    Silver        int       `json:"silver"`
    Grey          int       `json:"grey"`
    Black         int       `json:"black"`
    Red           int       `json:"red"`
    Maroon        int       `json:"maroon"`
    Orange        int       `json:"orange"`
    Yellow        int       `json:"yellow"`
    Olive         int       `json:"olive"`
    Lime          int       `json:"lime"`
    Green         int       `json:"green"`
    Aqua          int       `json:"aqua"`
    Teal          int       `json:"teal"`
    Blue          int       `json:"blue"`
    Navy          int       `json:"navy"`
    Fuchsia       int       `json:"fuchsia"`
    Purple        int       `json:"purple"`
    White         int       `json:"white"`

    Created       time.Time     `json:"-"`
    Updated       time.Time     `json:"-"`
}

func init() {
  http.HandleFunc("/build", build)
  http.HandleFunc("/list", list)
  http.HandleFunc("/artworks", artworks)
  http.HandleFunc("/process", process)
  http.HandleFunc("/oauth2callback", oauth2callback)
  http.HandleFunc("/doauth2callback", doauth2callback)
  http.HandleFunc("/submit_caption", submit_caption)
  http.HandleFunc("/up_caption", up_caption)
  http.HandleFunc("/down_caption", down_caption)
  http.HandleFunc("/computevotes", computevotes)
  http.HandleFunc("/captions", captions)
  http.HandleFunc("/computecaptions", computecaptions)
  http.HandleFunc("/download", download)
}

func download(w http.ResponseWriter, r *http.Request) {
  aid := r.FormValue("aid")
  gid := r.FormValue("gid")

  c := appengine.NewContext(r)

  if !assert_aid_exist(c, aid) {
    fmt.Fprintf(w, "{\"error\":\"aid not found\"}")
    return
  }

  if !assert_gid_exist(c, gid) {
    fmt.Fprintf(w, "{\"error\":\"gid not found\"}")
    return
  }

  q := datastore.NewQuery("artwork").Filter("Aid =", aid)
  var artworks []Artwork
  q.GetAll(c, &artworks)


  q = datastore.NewQuery("user").Filter("Gid =", gid)
  var users []User
  keys, _ := q.GetAll(c, &users)

  users[0].Downloads ++
  users[0].Id = keys[0].IntID()
  users[0].save(c)

  http.Redirect(w, r, artworks[0].Url, 301)
}

func artworks(w http.ResponseWriter, r *http.Request) {

  color := r.FormValue("color")
  sort := r.FormValue("sort")
  free := r.FormValue("extra")
  key := "artworks?color=" + color + "&extra=" + free

  c := appengine.NewContext(r)

  // Get the item from the memcache
  if artworks, err := memcache.Get(c, key); err == memcache.ErrCacheMiss {
      c.Infof("item %q not in the cache", key)

      var q *datastore.Query
      if color != "" && color != "all" {
        if free == "true" {
            q = datastore.NewQuery("artwork").Filter("Hidden =", false).Filter("Free =", true).Order("-" + UpcaseInitial(color))
        } else {
            q = datastore.NewQuery("artwork").Filter("Hidden =", false).Order("-" + UpcaseInitial(color))
        }
      } else {
        if free == "true" {
            q = datastore.NewQuery("artwork").Filter("Hidden =", false).Filter("Free =", true)
        } else if sort == "no" {
            q = datastore.NewQuery("artwork").Filter("Hidden =", false)
        } else {
            q = datastore.NewQuery("artwork").Filter("Hidden =", false).Order("Updated")
        }
      }

      t := q.Run(c)

      var cachedBuffer bytes.Buffer
      cached := bufio.NewWriter(&cachedBuffer)
      cachedAndResponse := io.MultiWriter(w, cached)

      enc := json.NewEncoder(cachedAndResponse)

      fmt.Fprintf(cachedAndResponse, "[")
      i := 0
      for ; ; {
        var x Artwork
        _, err := t.Next(&x)
        if err == datastore.Done {
                break
        }
        if(i > 0) {
          fmt.Fprintf(cachedAndResponse, ",")
        }
        if err != nil {
          c.Errorf("error with datastore: %v", err)
          http.Error(w, err.Error(), http.StatusInternalServerError)
          return
        }
        if err := enc.Encode(&x); err != nil {
          c.Errorf("error with encode: %v", err)
          http.Error(w, err.Error(), http.StatusInternalServerError)
          return
        }

        i++

        //if i == 20 {
        //    break
        //}
      }
      fmt.Fprintf(cachedAndResponse, "]")


        cached.Flush()

        // Create an Item
        item := &memcache.Item{
            Key:   key,
            Value: cachedBuffer.Bytes(),
        }
        if err := memcache.Add(c, item); err == memcache.ErrNotStored {
          c.Infof("item with key %q already exists", item.Key)
        } else if err != nil {
          c.Errorf("error adding item: %v", err)
        } else {
          c.Infof("item with key %q cached", item.Key)
        }

    } else if err != nil {
        c.Errorf("error getting artwork: %v", err)
    } else {

        // in cache
        fmt.Fprintf(w, string(artworks.Value))
    }
}

func list(w http.ResponseWriter, r *http.Request) {
  fmt.Fprintf(w, "{\"artworks\":")
  artworks(w, r)
  fmt.Fprintf(w, "}")
}


func process(w http.ResponseWriter, r *http.Request) {
  c := appengine.NewContext(r)
  imageHash := r.FormValue("h")
  processImage(c, imageHash)
}


func build(w http.ResponseWriter, r *http.Request) {
  c := appengine.NewContext(r)

  imageHashes := []string{
    "bYKUtAV.jpg","5WVktYO.jpg","ZLFpbxm.jpg","dyXbuVK.jpg","zEYl5Cp.jpg","0byg3HK.jpg","O8pQHDY.jpg","yZxeGfq.png","OVlKiK7.png","KzUPWxF.jpg","wCqGzTU.png","Eh79054.jpg","klGyEd6.png","omNQMoZ.jpg","RmPrDRL.jpg","Vg3Ruyl.jpg","CliUfqX.jpg","N6tlFJc.jpg","g5Q1NCa.jpg","1ZmrLjK.jpg","tLH51iW.jpg","QQLEwzF.jpg","w9fNuEX.jpg","mUOUz2Y.jpg","pYHoTtp.jpg","rC7mw31.jpg","i1Lv4CY.jpg","hZZRnoR.jpg","2hvl6qP.jpg","9VopmwA.jpg","CMTYkgj.jpg","v9mnBmP.jpg","Mly98OD.jpg","NK4ZyuA.jpg","BB1YAfb.jpg","CpSFYwY.jpg","DC4L7zI.jpg","nxuSi9d.jpg","MEej3vQ.jpg","OKI3GDJ.jpg","4OTXXx5.jpg","09EwMmq.jpg","xXpKKpU.jpg","bNsIXto.jpg","aodIouB.jpg","3wWtNXJ.jpg","530ygYW.jpg","TieinE6.jpg","vWSioof.jpg","OLwHF9J.jpg","NTN0jVS.jpg","yIFFaRt.jpg","sfb9PbR.jpg","d3ca61p.jpg","XU3xQNh.jpg","yMSLTPj.jpg","5Jz8lse.jpg","ApzUMsW.jpg","GXg3FUs.jpg","cwWI4AG.jpg","O1R2quB.jpg","fnjieyM.jpg","f9mYEvO.jpg","FzrnX0c.jpg","fF4PYMn.jpg","LsUvp5g.jpg","vQE6pwk.jpg","oL3ETVL.jpg","JiACR7N.jpg","8GloK0A.jpg","Zq7dJpw.jpg","XcjkS7n.jpg","qEalfZp.jpg","j2LDHX0.png","WzeMtiZ.jpg","tfFaE9N.png","td2y5xn.png","cwLnRHt.png","QLde8NU.png","dMobJqL.png","COg47x2.png","2e61QCd.jpg","GdW27Qi.png","KmFVtFp.png","NijSwjN.png","2yGlxuX.png","OYIHgN8.png","eANuDA4.png","M3vPdX7.png","KJ1eMqF.jpg","iagWrWh.jpg","AjtPWcv.png","iGVGeCd.png","7TctZPW.jpg","NwcOR6b.png","kLU0Rzr.png","nK8o9QA.png","yj5rVKB.png","uTPHRbC.png","BgiK6XN.png","SW6BzXV.png","OijO1Y9.png","GjxEpBz.png","KlhtVEY.png","7b119p3.png","Awp9gXz.png","jEz7hGz.png","G619cnJ.png","WKUAyyL.png","gjmbZXv.png","V2eZQPm.jpg","JRrm5bv.jpg","HiZkJDd.png","09Q7PHT.png","L4VNWYd.jpg","Zn2BBg8.png","bIhKjAb.png","hDCyDJt.png","ffKIEVx.png","7g9tvL1.png","FtobsA6.png","408ErZS.jpg","yz59p0x.png","YAURCyY.png","458cCPY.png","YLqxB95.jpg","vqwZC8R.png","5b8rP95.png","SOlE4lK.png","DTUqDGp.jpg","SnzwegU.png","n646Ctm.png","fdiO61M.jpg","AzK7wCT.jpg","CFn9TEx.jpg","2smhxyV.png","8gvwnAW.jpg","m6mCMOM.png","jmkCCjm.png","LnFOXBB.png","MxwmpVi.png","V54xpAK.jpg","6fkroYe.png","sdzaogp.png","gFhI5m7.jpg","MFQ2Obs.jpg","Wx4ay71.png","JreENo5.jpg","5MYm14y.jpg","VH2zETv.jpg","yFxrS7W.png","qkue89i.jpg","b1HAMmT.jpg","HJ2GW3n.jpg","jMDJxFJ.png","HChSD6f.png","zlpt78g.jpg","x8fkX0C.png","Vu4CobV.jpg","PF0onoL.png","Jjq7Vm4.png","8ncvhZx.png","OTTMzce.png","9wjW1kt.png","pOV1vvI.jpg","WS7bRQX.jpg","ecrMCre.jpg","hWcTKDR.png","DJ2egHW.png","EGpYBlc.jpg","OtqOw2M.png","2OP0oO2.png","TsrHVE0.png",
    "https://41.media.tumblr.com/a0fe0c41fed2840123440503f4ce2f6f/tumblr_n8pkofW7Rr1rdnwxgo1_1280.png","https://41.media.tumblr.com/978e45223b38e374614cda3193ee039d/tumblr_n80hrvLAiK1rdnwxgo1_1280.png","https://40.media.tumblr.com/83c7233c371b6db8fd79f3580f932a8f/tumblr_n56sspvL7i1rdnwxgo2_1280.png","https://36.media.tumblr.com/c25f8c6fff509cca346010c1360ad725/tumblr_n56sspvL7i1rdnwxgo3_1280.png","https://41.media.tumblr.com/79e3017d24437bdeb3086f0402c41a25/tumblr_n56sspvL7i1rdnwxgo4_1280.png","https://36.media.tumblr.com/782a0ebfe8500a816a8440b63bddbcb7/tumblr_n56sspvL7i1rdnwxgo1_1280.png","https://41.media.tumblr.com/819a970f4ca5b32fdc96a05525c4497b/tumblr_n4rb3qIdVd1rdnwxgo1_1280.png","https://41.media.tumblr.com/dd0fde4506ed58bdf93e6ed1e53dde9f/tumblr_n4rajxdvkX1rdnwxgo1_1280.png","https://41.media.tumblr.com/3d3fa2195c7d4e1c2e55fbcb3ccb260e/tumblr_n4raigTeYz1rdnwxgo1_1280.png","https://40.media.tumblr.com/80d33c9e476d4db6d8b9fe9636f3521f/tumblr_n4rahbowab1rdnwxgo1_1280.png","https://40.media.tumblr.com/82ec058eef5d34cf178b38ee9439c330/tumblr_n4raddepDh1rdnwxgo1_1280.png","https://41.media.tumblr.com/21c79bc23cfc157d6b5173bacbdbc533/tumblr_n4raba080h1rdnwxgo1_1280.png","https://41.media.tumblr.com/55587d3d65b83abb423603a00f9d1389/tumblr_n4ral51o971rdnwxgo1_1280.png","https://41.media.tumblr.com/3c89e6ee1a62fd31b4b717c43411a0f4/tumblr_n4r9iuNoqr1rdnwxgo1_1280.png","https://40.media.tumblr.com/93fa53116cc6747da8856c1999f7941e/tumblr_n4r9iuNoqr1rdnwxgo2_1280.png","https://36.media.tumblr.com/605396ad3ed5e3830e6b3d6826ff9d60/tumblr_n4r8xniqxE1rdnwxgo1_1280.png","https://41.media.tumblr.com/8a6f7539ff4455c7f1fae743eac559a1/tumblr_n4r8usTbFI1rdnwxgo1_1280.png","https://41.media.tumblr.com/c0873689184c414e8aac23a0cc39bb84/tumblr_n4r8usTbFI1rdnwxgo2_1280.png","https://41.media.tumblr.com/c2f7468e1b77fb2f90f9dbcfd37c93c9/tumblr_n4r8mxJqFb1rdnwxgo1_1280.png","https://36.media.tumblr.com/0d7e88cd559235b41336de29e83604b3/tumblr_n4r80wqUH81rdnwxgo1_1280.png","https://40.media.tumblr.com/aa01bdeef913c4e4687edb7afc3bdff5/tumblr_n4r80wqUH81rdnwxgo2_r1_1280.png","https://41.media.tumblr.com/cc72b56f00ecca4b46e72b601e57c978/tumblr_n4r7vlMwnX1rdnwxgo1_1280.png","https://41.media.tumblr.com/5bb2bc1b2bda0875ceeab756eaf16c80/tumblr_myuko72CMx1rmhjryo1_1280.png","https://40.media.tumblr.com/8387d071770e67467bace574d7396fc3/tumblr_n2yzqaW2Qj1rdnwxgo3_1280.png","https://40.media.tumblr.com/56e8c2c2f350455c7b60cd1f73e15800/tumblr_n2yzqaW2Qj1rdnwxgo1_1280.png","https://36.media.tumblr.com/0392ae613d44f02f37610bfc53934f12/tumblr_n2yzqaW2Qj1rdnwxgo2_1280.png","https://40.media.tumblr.com/a6658f5839ca48c1ad46255827656e3e/tumblr_n2yz3pQ0zA1rdnwxgo2_1280.png","https://40.media.tumblr.com/86c5c2f9a922880c7b863cc44ca0a902/tumblr_n2yz3pQ0zA1rdnwxgo1_1280.png","https://41.media.tumblr.com/207999a60981a93fac82d9c80c3c60fa/tumblr_n2pk1rGav91rdnwxgo2_1280.png","https://36.media.tumblr.com/bb98082ad278c0f37602ea24a92b62da/tumblr_n2pk1rGav91rdnwxgo3_1280.png","https://40.media.tumblr.com/a113da8771de949462a96274d4d8ebd8/tumblr_n2pk1rGav91rdnwxgo1_1280.png","https://36.media.tumblr.com/4d9f19e2b682e159836363f9f996946d/tumblr_n2pj8s3v8Q1rdnwxgo1_1280.png","https://41.media.tumblr.com/1d56c22b8d953d83e4bb69011684d409/tumblr_n2pj8s3v8Q1rdnwxgo2_1280.png","https://41.media.tumblr.com/b0b7662c7e09931ccb2a76f70d4520b9/tumblr_n2pj8s3v8Q1rdnwxgo3_1280.png","https://40.media.tumblr.com/fd1ea3214ae79b3a45a73f5c3e690597/tumblr_n2pihdmV2l1rdnwxgo1_1280.png","https://36.media.tumblr.com/e57179bdcd892eb4d137951632b29e30/tumblr_n2pihdmV2l1rdnwxgo2_1280.png","https://40.media.tumblr.com/ef12c0f54ab38556bd58f82da58a1ea2/tumblr_n2phpw6QkM1rdnwxgo2_1280.png","https://36.media.tumblr.com/d0901dfa8183f040d17355e5faf1ec72/tumblr_n2phpw6QkM1rdnwxgo1_1280.png","https://36.media.tumblr.com/a8cdbf6a60aeffc393d052c2b7c800aa/tumblr_n2pgrbrrTW1rdnwxgo1_1280.png","https://41.media.tumblr.com/ec27578fea12f2ca146dd928e42511e2/tumblr_n2pgomgB4q1rdnwxgo1_1280.png","https://41.media.tumblr.com/ba7215bcfe0d8fb0de8de675ebb67318/tumblr_n2pg11sXQf1rdnwxgo1_1280.png","https://41.media.tumblr.com/c43d6e409eb72a77c304e37e879f1348/tumblr_n2pfjj9iii1rdnwxgo1_1280.png","https://40.media.tumblr.com/024ad39f954cb1c487d7fa8915442b05/tumblr_mppyupjYUd1rdnwxgo1_1280.png","https://41.media.tumblr.com/e2e96862fba624af703ac5b6c8724d4a/tumblr_mppytitHA61rdnwxgo1_1280.png","https://41.media.tumblr.com/b3d3dd862b80cd17428f3d7b083e762d/tumblr_mppysaGV211rdnwxgo1_1280.png","https://40.media.tumblr.com/12e4554804955951f4abbd15736badcf/tumblr_mppyqkCtVA1rdnwxgo1_1280.png","https://41.media.tumblr.com/d4765fffb35d4f024714be435069d878/tumblr_mppypeJXFm1rdnwxgo1_1280.png","https://40.media.tumblr.com/fa104719b1e4922f05b8f28b6f7755fa/tumblr_mppynavaqr1rdnwxgo1_1280.png","https://40.media.tumblr.com/2646b416fa5a8976b6a42f21bf69177e/tumblr_mppylleMyf1rdnwxgo1_1280.png","https://40.media.tumblr.com/f3f61e478856d822008cd523416111f4/tumblr_mppyk0ivQH1rdnwxgo1_1280.png","https://40.media.tumblr.com/cea13a746df4af31da7da1862a6f2dfb/tumblr_mppyfiWBVk1rdnwxgo1_1280.png","https://40.media.tumblr.com/17ea548e2b9265c41a86c6816243c302/tumblr_mppyfiWBVk1rdnwxgo2_r1_1280.png","https://41.media.tumblr.com/100806d65d60da4f88f2f68b3df41aac/tumblr_mppyb7cBLL1rdnwxgo1_1280.png","https://41.media.tumblr.com/afdd8e8a3c03c250d5d5f313378ee9c0/tumblr_mppxeiixD11rdnwxgo1_1280.png","https://40.media.tumblr.com/e8b8d86daad752fe5872217b24e3ccde/tumblr_mppxbjWSMF1rdnwxgo1_1280.png","https://41.media.tumblr.com/92fb6cc1a65153d31fddc2888f1841be/tumblr_mppx4dJJAc1rdnwxgo1_1280.png","https://40.media.tumblr.com/740219ecffc3fc5273237ea9d7633d72/tumblr_mppwtzGbWa1rdnwxgo1_1280.png","https://41.media.tumblr.com/141efdd6665135f2e3430f105e0c5993/tumblr_mp8om2jEuH1rdnwxgo1_1280.png","https://41.media.tumblr.com/d728342daac7a200938c6f343eecea30/tumblr_mp8okvpwsH1rdnwxgo1_1280.png","https://41.media.tumblr.com/bed1794dcd898b2b8c97f8c07dd53194/tumblr_mp8ohsxKMB1rdnwxgo1_1280.png","https://41.media.tumblr.com/e3774d6af1dca0980fd7c272b68333ee/tumblr_mp8ogdVjgM1rdnwxgo1_1280.png","https://41.media.tumblr.com/f8bf3583a9c95c40743562ef4b106d32/tumblr_mp8o2q11Oz1rdnwxgo1_1280.png","https://41.media.tumblr.com/e0a77c3a55ad8bc8fb653578fa84b69e/tumblr_mp7wk5b3HG1rdnwxgo1_1280.png","https://41.media.tumblr.com/dffbbffd0a8c3425326fbe9501421457/tumblr_mp7wk5b3HG1rdnwxgo2_1280.png","https://40.media.tumblr.com/cf4d2f83a6b5cd74bcdfecab71b99d00/tumblr_mp7wf5nMyE1rdnwxgo1_1280.png","https://40.media.tumblr.com/81b983b46cb52b694d57c557a3c626bd/tumblr_mp7wb8XFIW1rdnwxgo1_1280.png","https://36.media.tumblr.com/452b977f1bd85e3b07361b1c865fc217/tumblr_mp7w8aosrH1rdnwxgo1_1280.png","https://40.media.tumblr.com/2345a89261242d9a711a283e4a3b0d1e/tumblr_mp7w5wJx5A1rdnwxgo1_1280.png","https://41.media.tumblr.com/f5f71dce0564c5d9194f420d5669a73e/tumblr_mp74a0oOf51rdnwxgo1_1280.png","https://41.media.tumblr.com/8625db34c66bf1159b6bdb49711f1c22/tumblr_mp748qNhW31rdnwxgo1_1280.png","https://40.media.tumblr.com/d0f5f6e8a4d7155ea0ede149e451f422/tumblr_mp7462Mk131rdnwxgo1_1280.png","https://41.media.tumblr.com/73bee2820cafbc7bd3e7c88ca5f225ab/tumblr_mp73wv4aAg1rdnwxgo1_1280.png","https://41.media.tumblr.com/c534eb7fdb2790bc188d1bcc4dd32faf/tumblr_mp73wv4aAg1rdnwxgo2_1280.png","https://36.media.tumblr.com/78ba98e09e08646dcb16d0035be004a8/tumblr_mp73tfDBul1rdnwxgo1_1280.png","https://36.media.tumblr.com/2abf32ce15e5e9e2101637628244e14f/tumblr_mp7w3eUysP1rdnwxgo1_1280.png","https://40.media.tumblr.com/72fcc0e04b3cca4c9f96bd55db38bbb1/tumblr_mp73lyJAJI1rdnwxgo1_1280.png","https://40.media.tumblr.com/fa0991c71f0019219c317f50f702c82f/tumblr_mp73321lw91rdnwxgo1_1280.png","https://41.media.tumblr.com/5dbb5307032d4e12462a021e322f8fea/tumblr_mp72yrbULo1rdnwxgo1_1280.png","https://36.media.tumblr.com/63b0bf83f93c48baa4a790053cf4ad1b/tumblr_mp726aGc5A1rdnwxgo1_1280.png","https://41.media.tumblr.com/d060c9fc7eb90ff3046470af49f86f7d/tumblr_mp724gfsok1rdnwxgo1_1280.png","https://41.media.tumblr.com/916d963c322a73edd1937a092b44ad5b/tumblr_mp73qjHhlU1rdnwxgo1_1280.png","https://40.media.tumblr.com/01d89df2fdb07f3cb32f068975e6af57/tumblr_mp722o75Ex1rdnwxgo1_1280.png","https://40.media.tumblr.com/4ecda8e2c747c8604891c2631d902bcb/tumblr_mp721699iH1rdnwxgo1_1280.png","https://40.media.tumblr.com/b6df64855a6237b8f2cf531f2c35abb1/tumblr_mp71zjGPm11rdnwxgo1_1280.png","https://41.media.tumblr.com/1cd22b502104bc766f996b1959ed39e2/tumblr_mp8oe8kvrK1rdnwxgo1_1280.png","https://41.media.tumblr.com/945115d4b7a93919f8506e4ecaed41ac/tumblr_mp716kwIZv1rdnwxgo2_1280.png","https://41.media.tumblr.com/292678ce33cf7979c93c8fc9d749c86f/tumblr_mp716kwIZv1rdnwxgo1_1280.png","https://36.media.tumblr.com/cfa1c050f1d2d8e6a60818b1acc53860/tumblr_mp6zx7h4QY1rdnwxgo1_1280.png","https://40.media.tumblr.com/b4a027c4bdd67053c388bffea1d3a1c4/tumblr_mp6zx7h4QY1rdnwxgo2_1280.png","https://41.media.tumblr.com/617e0e6af57a41852f83b0e5466313bc/tumblr_mp6zp0KDjX1rdnwxgo1_1280.png","https://41.media.tumblr.com/5208349cc0cffcb5fc049696626aa484/tumblr_mp74dnvxq51rdnwxgo1_1280.png","https://40.media.tumblr.com/31d31a6e4c36bd954e1fc2adfc140ef2/tumblr_mp74dnvxq51rdnwxgo2_1280.png","https://41.media.tumblr.com/9b8c380d8d61e3a5bd3ed4d4158a9ea8/tumblr_mp7w0yvKaD1rdnwxgo1_1280.png","https://41.media.tumblr.com/3337c13d22cc90f840fde70a7b10c13f/tumblr_mp7v3fplgH1rdnwxgo1_1280.png","https://40.media.tumblr.com/c5f0a3ddde83594de0d7e467ef3a9bc6/tumblr_mp7v0hnYEw1rdnwxgo1_1280.png","https://41.media.tumblr.com/03f83657006313b4cd6ddddc7c9537af/tumblr_mp7uiwqxIY1rdnwxgo1_1280.png","https://40.media.tumblr.com/a97939f29b111229b8c879701f7a77c3/tumblr_mp727yQiis1rdnwxgo1_1280.png","https://41.media.tumblr.com/e09ca0822b4f17c25ce937faad26159c/tumblr_mp74prkw6B1rdnwxgo1_1280.png","https://41.media.tumblr.com/0f41fcd85273f997e2ee4e3718e202c2/tumblr_mp72m7s32Z1rdnwxgo1_1280.png","https://41.media.tumblr.com/d2847cc727443d7728eb8c4729eb4c74/tumblr_mp72m7s32Z1rdnwxgo3_1280.png","https://36.media.tumblr.com/1d8df664be8ee5146a2270922dc26db1/tumblr_mp72m7s32Z1rdnwxgo2_1280.png","https://41.media.tumblr.com/aec65540dc0eb453271c120633f47ac1/tumblr_mp71beR5ot1rdnwxgo1_1280.png","https://40.media.tumblr.com/5d4bdff893e9545003bcbebd66ad998a/tumblr_mp71beR5ot1rdnwxgo2_1280.png","https://41.media.tumblr.com/7d508da06d956db0f85aacc9cb6ace69/tumblr_mp71beR5ot1rdnwxgo3_1280.png","https://41.media.tumblr.com/0a52700d124dfc542e40420fe22b7fef/tumblr_mp71beR5ot1rdnwxgo4_1280.png","https://41.media.tumblr.com/b34078bf73f85876613fa97dd688e8dc/tumblr_mp6zd19GK91rdnwxgo2_1280.png","https://41.media.tumblr.com/af2860e1c26a3edbb8ac044fb58e0c66/tumblr_mp6zd19GK91rdnwxgo3_1280.png","https://41.media.tumblr.com/0fb14e30a6f6372906e452da21818a28/tumblr_mp6zd19GK91rdnwxgo4_1280.png","https://41.media.tumblr.com/de60bc2c8e5f8ac1842eb3a03964c8c1/tumblr_mp6zd19GK91rdnwxgo1_1280.png","https://41.media.tumblr.com/b6fd3af360287b208463eb7c8505ea9b/tumblr_mp6y88yy601rdnwxgo1_1280.jpg","https://40.media.tumblr.com/0cd708ab97b501adcba75fd82b35845e/tumblr_mp6y88yy601rdnwxgo2_1280.jpg","https://41.media.tumblr.com/811b0c42c6b3c8a118cf80c5f66f96a7/tumblr_mp6xosruXO1rdnwxgo3_1280.png","https://40.media.tumblr.com/8a4fa7045edd5a8676975d7cae1214b2/tumblr_mp6xosruXO1rdnwxgo1_1280.png","https://41.media.tumblr.com/fc97c0a3631eae56afd4fd753b2196eb/tumblr_mp6xosruXO1rdnwxgo2_1280.png","https://41.media.tumblr.com/9c764b0a9173e426515581b5a2ba26ff/tumblr_mp6wz8PrED1rdnwxgo2_1280.png","https://40.media.tumblr.com/42aa49689b244c2b259dd50afdb8e37f/tumblr_mp6wz8PrED1rdnwxgo1_1280.png","https://41.media.tumblr.com/473cb9ae2da7ede995e1014c5b501d94/tumblr_mp6wr3ZtDA1rdnwxgo1_1280.png","https://41.media.tumblr.com/852d90bc06bfe8d0b48fa782a2425f86/tumblr_mp6wr3ZtDA1rdnwxgo2_1280.png","https://36.media.tumblr.com/abda47dc649e680152b84fdadbb14d5a/tumblr_mp6wr3ZtDA1rdnwxgo3_1280.png","https://40.media.tumblr.com/fab231f9922816b3d6879f094b0ccbac/tumblr_mp6wr3ZtDA1rdnwxgo5_1280.png","https://41.media.tumblr.com/2ff648e1129c543bee673a159005cfbc/tumblr_mp6wr3ZtDA1rdnwxgo4_1280.png","https://40.media.tumblr.com/5a6126604c0750ed81658bdea425556c/tumblr_mp6uaaz3lL1rdnwxgo3_1280.png","https://40.media.tumblr.com/ef90501656f7c9b55c7eb658a6772327/tumblr_mp6uaaz3lL1rdnwxgo2_1280.png","https://40.media.tumblr.com/151f081cfbce8d4724334246ec2e8c37/tumblr_mp6uaaz3lL1rdnwxgo1_1280.png","https://36.media.tumblr.com/c0f1efe0e3fad5fa32c329537b9f2be9/tumblr_mp6uaaz3lL1rdnwxgo4_1280.png","https://40.media.tumblr.com/62a297c07b1dd1adea2eb08999d69459/tumblr_mgxv38bBw01rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_mac5nsdMiP1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_mac5nsdMiP1rdnwxgo2_1280.png","https://41.media.tumblr.com/tumblr_m8uqtmgYcz1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8uqtmgYcz1rdnwxgo2_1280.png","https://40.media.tumblr.com/tumblr_m8uqic5OCX1rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8uqic5OCX1rdnwxgo2_1280.png","https://41.media.tumblr.com/tumblr_m8pv9nZltY1rdnwxgo1_1280.png","https://36.media.tumblr.com/tumblr_m8pv9nZltY1rdnwxgo2_1280.png","https://41.media.tumblr.com/tumblr_m8pv9nZltY1rdnwxgo3_1280.png","https://40.media.tumblr.com/tumblr_m8pv9nZltY1rdnwxgo4_1280.png","https://41.media.tumblr.com/tumblr_m8pune9sNJ1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8pune9sNJ1rdnwxgo2_1280.png","https://40.media.tumblr.com/tumblr_m8pune9sNJ1rdnwxgo3_1280.png","https://36.media.tumblr.com/tumblr_m8m46gwUEl1rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8m41oAWak1rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8m3xtn51w1rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8j17wqHCA1rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8j13xZkq61rdnwxgo1_1280.png","https://36.media.tumblr.com/tumblr_m8j0z7dIme1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8m35cnskR1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8m35cnskR1rdnwxgo2_1280.png","https://40.media.tumblr.com/tumblr_m8m35cnskR1rdnwxgo3_1280.png","https://41.media.tumblr.com/tumblr_m8m35cnskR1rdnwxgo4_1280.png","https://41.media.tumblr.com/tumblr_m8m35cnskR1rdnwxgo5_1280.png","https://41.media.tumblr.com/tumblr_m8m35cnskR1rdnwxgo6_r1_1280.png","https://36.media.tumblr.com/tumblr_m8m35cnskR1rdnwxgo7_1280.png","https://41.media.tumblr.com/tumblr_m8j06ssNSh1rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8j03jddaX1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8j002ybD81rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8izvmgNl61rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8izogEIkN1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8izjbClqI1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8izg9dpJe1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8izdm8QMW1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8iza47Ee81rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8gv5ko6X31rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8ggncsmeX1rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8gffyYNKJ1rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8gx0dzUal1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8gx0dzUal1rdnwxgo2_1280.png","https://40.media.tumblr.com/tumblr_m8gx0dzUal1rdnwxgo3_1280.png","https://41.media.tumblr.com/tumblr_m8gevjz3HT1rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8fwtirG8m1rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8ezkgefiZ1rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8ezkgefiZ1rdnwxgo2_1280.png","https://40.media.tumblr.com/tumblr_m8el95d69u1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8fxrsoNeW1rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8fxrsoNeW1rdnwxgo2_1280.png","https://40.media.tumblr.com/tumblr_m8fxrsoNeW1rdnwxgo3_1280.png","https://41.media.tumblr.com/tumblr_m8fxgvqyTz1rdnwxgo1_1280.png","https://36.media.tumblr.com/tumblr_m8fxgvqyTz1rdnwxgo2_1280.png","https://41.media.tumblr.com/tumblr_m8fxgvqyTz1rdnwxgo3_1280.png","https://40.media.tumblr.com/tumblr_m8fxgvqyTz1rdnwxgo4_1280.png","https://41.media.tumblr.com/tumblr_m8d33m3iKx1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8ezeaqDpW1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8ez5bwmYN1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8ez1r85Dx1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8eyrahU8u1rdnwxgo1_1280.png","https://36.media.tumblr.com/tumblr_m8d2u7x25e1rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8epohtqD51rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8cy4oXBKJ1rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8cy4oXBKJ1rdnwxgo2_1280.png","https://40.media.tumblr.com/tumblr_m8cwrzGqb91rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8cwgaCSWZ1rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8d26eILNm1rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8cvpqhaAd1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8cw1nCzVm1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8cw1nCzVm1rdnwxgo2_1280.png","https://41.media.tumblr.com/tumblr_m8cvkxvuUE1rdnwxgo1_1280.png","https://41.media.tumblr.com/tumblr_m8crejynDl1rdnwxgo1_1280.png","https://40.media.tumblr.com/tumblr_m8crejynDl1rdnwxgo2_1280.png"}

  for i := 0;  i<len(imageHashes); i++ {

      q := datastore.NewQuery("artwork").Filter("Hash =", imageHashes[i])
      var artworks []Artwork
      _, err := q.GetAll(c, &artworks)
      if err != nil {
         c.Errorf("Error GetAll Artwork for %q: %v\n",imageHashes[i], err)
         return
      }

      if len(artworks) == 0 {
          c.Infof("process %q\n",imageHashes[i])
          t := taskqueue.NewPOSTTask("/process", map[string][]string{"h": {imageHashes[i]}})
          if _, err := taskqueue.Add(c, t, "process"); err != nil {
              http.Error(w, err.Error(), http.StatusInternalServerError)
              return
          }
          c.Infof("%q added in queue\n", imageHashes[i])
      }

  }

}

func processImage(c appengine.Context, imageUrl string) {
  client := urlfetch.Client(c)
  imageHash := imageUrl
  imageUrlSmall := imageUrl
  root := "https://i.imgur.com/"

  if !strings.HasPrefix(imageUrl, "http") {
    c.Infof("imgur image %q\n", imageUrl)
    imageUrl = root + imageUrl
    if strings.HasSuffix(imageUrl, ".jpg") {
        imageUrlSmall = strings.Replace(imageUrl, ".jpg", "m.jpg", -1)
    } else {
        imageUrlSmall = strings.Replace(imageUrl, ".png", "m.png", -1)
    }

  } else {

    c.Infof("tumblr image %q\n", imageUrl)
    imageUrlSmall = strings.Replace(imageUrl, "_1280.png", "_500.png", -1)

  }

  resp, err := client.Get(imageUrl)
  if err != nil {
    c.Errorf("Error loading image %q: %v\n", imageUrl, err)
  } else {

    var image image.Image
    defer resp.Body.Close()
    if strings.HasSuffix(imageUrl, ".png") {
        var err error
        image, err = png.Decode(resp.Body)
        if err != nil {
          c.Errorf("Error decoding image %q: %v\n", imageUrl, err)
        }
    } else {
        var err error
        image, err = jpeg.Decode(resp.Body)
        if err != nil {
            c.Errorf("Error decoding image %q: %v\n", imageUrl, err)
        }
    }

    if image != nil {
        c.Infof("%q : %d - %d\n", imageUrl, image.Bounds().Max.X, image.Bounds().Max.Y)
        ratio := float32(image.Bounds().Max.X) / float32(image.Bounds().Max.Y)
        ratio = float32(int(ratio * 1000)) / 1000


        c1 := Artwork {
            Hash:     imageHash,
            Url:      imageUrl,
            UrlSmall: imageUrlSmall,
            Width:    image.Bounds().Max.X,
            Height:   image.Bounds().Max.Y,
            Ratio:    ratio,
        }

        image = resize.Resize(100, 0, image, resize.Bilinear)
        bounds := image.Bounds()

        ColorCounter := make(map[string]int)

        TotalPixels := bounds.Max.X * bounds.Max.Y

        for i := 0; i <= bounds.Max.X; i++ {
          for j := 0; j <= bounds.Max.Y; j++ {
               pixel := image.At(i, j)
               red, green, blue, _ := pixel.RGBA()
               RGBTuple := []int{int(red/255), int(green/255), int(blue/255)}
               ColorName := FindClosestColor(RGBTuple, "css21")
               _, present := ColorCounter[ColorName]
               if present {
                    ColorCounter[ColorName] += 1
               } else {
                    ColorCounter[ColorName] = 1
               }
          }
        }

        // Sort by the frequency of each color
        keys := make([]int, 0, len(ColorCounter))
        for _, val := range ColorCounter {
          keys = append(keys, val)
        }
        sort.Sort(sort.Reverse(sort.IntSlice(keys)))

        ReverseColorCounter := ReverseMap(ColorCounter)

        // Display the top N dominant colors from the image
        for _, val := range keys[:len(keys)] {

          color := UpcaseInitial(ReverseColorCounter[val])
          value := ((float64(val) / float64(TotalPixels)) * 100);

          intValue := int64(value * 1000)

          c.Infof("%s %d\n", color, intValue)
          reflect.ValueOf(&c1).Elem().FieldByName(color).SetInt(intValue)
        }

        if strings.HasPrefix(c1.Hash, "http") {
            c1.Free = false
        } else {
            c1.Free = true
        }

        c1.save(c)
        c1.save(c)
    }
  }
}

func UpcaseInitial(str string) string {
    for i, v := range str {
        return string(unicode.ToUpper(v)) + str[i+1:]
    }
    return ""
}

func (t *Artwork) key(c appengine.Context) *datastore.Key {
  if t.Id == 0 {
    t.Created = time.Now()
    c.Infof("\tcreating a new Artwork\n")
    return datastore.NewIncompleteKey(c, "artwork", nil)
  }
  c.Infof("\tupdating a new Artwork\n")
  return datastore.NewKey(c, "artwork", "", t.Id, nil)
}

func (t *Artwork) save(c appengine.Context) (*Artwork, error) {
  t.Updated = time.Now()
  k, err := datastore.Put(c, t.key(c), t)
  if err != nil {
    return nil, err
  }
  if k.IntID() != 0 {
    buf := base58.EncodeBig(nil, big.NewInt(k.IntID()))
    t.Aid = string(buf)
    t.Id = k.IntID()
    c.Infof("\t> key is %s\n", t.Aid)
  }
  return t, nil
}

// This method finds the closest color for a given RGB tuple and returns the name of the color in given mode
func FindClosestColor(RequestedColor []int, mode string) string {
     MinColors := make(map[int]string)
     var ColorMap map[string]string

     // css3 gives the shades while css21 gives the primary or base colors
     if mode == "css3" {
          ColorMap = gwc.CSS3NamesToHex
     } else {
          ColorMap = gwc.HTML4NamesToHex
     }

     for name, hexcode := range ColorMap {
          rgb_triplet := gwc.HexToRGB(hexcode)
          rd := math.Pow(float64(rgb_triplet[0] - RequestedColor[0]), float64(2))
          gd := math.Pow(float64(rgb_triplet[1] - RequestedColor[1]), float64(2))
          bd := math.Pow(float64(rgb_triplet[2] - RequestedColor[2]), float64(2))
          MinColors[int(rd + gd + bd)] = name
     }

     keys := make([]int, 0, len(MinColors))
     for key := range MinColors {
          keys = append(keys, key)
     }
     sort.Ints(keys)
     return MinColors[keys[0]]
}

// This method creates a reverse map
func ReverseMap(m map[string]int) map[int]string {
    n := make(map[int]string)
    for k, v := range m {
        n[v] = k
    }
    return n
}