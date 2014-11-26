package muzeighibli

import (
    "io"
    "fmt"
	"bytes"
    "bufio"
    "encoding/json"
    "time"
    "math/big"
    "net/http"
    "strconv"
    "github.com/tv42/base58"
    "appengine"
    "appengine/taskqueue"
    "appengine/datastore"
    "appengine/memcache"
)

type Vote struct {
    Id            int64         `json:"id" datastore:"-"`
    Cid           string        `json:"cid"`
    Gid           string        `json:"gid"`
    Up            bool          `json:"up"`
    Created       time.Time     `json:"-"`
    Updated       time.Time     `json:"-"`
}

type Feature struct {
    Id            int64         `json:"id" datastore:"-"`
    Fid           string        `json:"fid"`
    Title         string        `json:"title"`
    DisplayTitle  string        `json:"dislay"`
    Subtitle      string        `json:"subtitle"`
}

type Caption struct {
    Id            int64         `json:"id" datastore:"-"`
    Cid           string        `json:"cid"`
    Gid           string        `json:"gid"`
    Aid           string        `json:"aid"`
    Language      string        `json:"lang"`
    Votes         int           `json:"votes"`
    Caption       string        `json:"caption"`
    Created       time.Time     `json:"-"`
    Updated       time.Time     `json:"-"`
}

func captions(w http.ResponseWriter, r *http.Request) {
  gid := r.FormValue("gid")

  c := appengine.NewContext(r)

    if !assert_gid_exist(c, gid) {
        fmt.Fprintf(w, "{\"error\":\"gid not found\"}")
        return
    }

  key := "captions-" + gid

  // Get the item from the memcache
  if captions, err := memcache.Get(c, key); err == memcache.ErrCacheMiss {
      c.Infof("item %q not in the cache", key)

      var q *datastore.Query

      q = datastore.NewQuery("caption").Filter("Gid =", gid)

      t := q.Run(c)

      var cachedBuffer bytes.Buffer
      cached := bufio.NewWriter(&cachedBuffer)
      cachedAndResponse := io.MultiWriter(w, cached)

      enc := json.NewEncoder(cachedAndResponse)

      fmt.Fprintf(cachedAndResponse, "[")
      i := 0
      for ; ; {
        var x Caption
        _, err := t.Next(&x)
        if err == datastore.Done {
                break
        }
        if(i > 0) {
          fmt.Fprintf(cachedAndResponse, ",")
        }
        if err != nil {
          http.Error(w, err.Error(), http.StatusInternalServerError)
          return
        }
        if err := enc.Encode(&x); err != nil {
          http.Error(w, err.Error(), http.StatusInternalServerError)
          return
        }

        i++
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
        c.Errorf("error getting caption: %v", err)
    } else {

        // in cache
        fmt.Fprintf(w, string(captions.Value))
    }
}

func up_caption(w http.ResponseWriter, r *http.Request) {
    vote_caption(w, r, true)
}

func down_caption(w http.ResponseWriter, r *http.Request) {
    vote_caption(w, r, false)
}

func vote_caption(w http.ResponseWriter, r *http.Request, up bool) {
    cid := r.FormValue("cid")
    gid := r.FormValue("gid")

    c := appengine.NewContext(r)

    if !assert_cid_exist(c, cid, gid) {
        fmt.Fprintf(w, "{\"error\":\"cid not found\"}")
        return
    }
    if !assert_gid_exist(c, gid) {
        fmt.Fprintf(w, "{\"error\":\"gid not found\"}")
        return
    }

    q := datastore.NewQuery("vote").Filter("Cid =", cid).Filter("Gid =", gid)
    var votes []Vote
    vote_keys, err := q.GetAll(c, &votes)
    if err != nil {
        c.Errorf("Error GetAll Vote for %q: %v\n", cid, err)
        return
    }
    if len(votes) == 1 && votes[0].Up == up {
        if(up) {
            add_vote(c, cid, -1)
        } else {
            add_vote(c, cid, 1)
        }
        datastore.Delete(c,vote_keys[0])
        fmt.Fprintf(w, "{\"status\":\"ok\"}")
    } else {
        if(up) {
            if len(votes) == 1 {
                add_vote(c, cid, 2)
            } else {
                add_vote(c, cid, 1)
            }
        } else {
            if len(votes) == 1 {
                add_vote(c, cid, -2)
            } else {
                add_vote(c, cid, -1)
            }
        }
        vote := Vote {
            Gid:        gid,
            Cid:        cid,
            Up:         up,
        }
        if len(votes) == 1 {
           vote.Id = vote_keys[0].IntID()
        }
        vote.save(c)
        fmt.Fprintf(w, "{\"status\":\"ok\"}")
    }
}

func add_vote(c appengine.Context, cid string, diff int) {
    t := taskqueue.NewPOSTTask("/computevotes", map[string][]string{"cid": {cid}, "diff": {strconv.Itoa(diff)}})
    if _, err := taskqueue.Add(c, t, "process"); err != nil {
        c.Errorf("Error adding in queue %q: %v\n", cid, err)
        return
    }
    c.Infof("%q added in queue\n", cid)
}

func computecaptions(w http.ResponseWriter, r *http.Request) {

    c := appengine.NewContext(r)


    q := datastore.NewQuery("feature")
    var list []Feature
    key_art, err := q.GetAll(c, &list)
    if err != nil {
        c.Errorf("Error GetAll Feature : %v\n", err)
        return
    }
    features := make(map[string]Feature)
    for i := 0; i < len(list); i++ {
        c.Infof("%q -> %q : %q\n", list[i].Title, list[i].DisplayTitle, list[i].Subtitle)
        features[list[i].Title] = list[i]
    }

    q = datastore.NewQuery("artwork").Filter("Hidden =", false)

    var artworks []Artwork
    key_art, err = q.GetAll(c, &artworks)
    if err != nil {
        c.Errorf("Error GetAll Artwork : %v\n", err)
        return
    }

    for i := 0; i < len(artworks); i++ {

        artwork := artworks[i]

        q = datastore.NewQuery("caption").Filter("Aid =", artwork.Aid)

        var captions []Caption
        _, err := q.GetAll(c, &captions)
        if err != nil {
            c.Errorf("Error GetAll Caption for %q: %v\n", artwork.Aid, err)
            return
        }

        artworkCaption := artwork.Caption
        m := make(map[string]int)
        max := 0

        for j := 0; j < len(captions); j++ {

            caption := captions[j]

            i, ok := m[caption.Caption]
            if(ok) {
                m[caption.Caption] = i+1
            } else {
                m[caption.Caption] = 1
            }

            if m[caption.Caption] > max {
                artworkCaption = caption.Caption
                max = m[caption.Caption]
            }
        }

        feature := features[artworkCaption]
        if max > 2 && (feature.DisplayTitle != artwork.Caption || feature.Subtitle != artwork.Subtitle) {
            c.Infof("%q choosen by %d -> %q\n", artwork.UrlSmall, max, artworkCaption)
            artwork.Id = key_art[i].IntID()
            artwork.Caption = feature.DisplayTitle
            artwork.Subtitle = feature.Subtitle
            artwork.save(c)
        }

    }
}

func computevotes(w http.ResponseWriter, r *http.Request) {
    c := appengine.NewContext(r)
    cid := r.FormValue("cid")
    diff, err := strconv.Atoi(r.FormValue("diff"))
    if err != nil {
        c.Errorf("Error converting diff %q: %v\n", r.FormValue("diff"), err)
        return
    }
    q := datastore.NewQuery("caption").Filter("Cid =", cid)
    var captions []Caption
    cap_keys, err := q.GetAll(c, &captions)
    if err != nil {
        c.Errorf("Error GetAll Caption for %q: %v\n", cid, err)
        return
    }
    if len(captions) == 1 {
        caption := captions[0]
        caption.Id = cap_keys[0].IntID()
        caption.Votes += diff
        caption.save(c)
        user := get_user(c, caption.Gid);
        if user != nil {
            user.Score += diff;
            user.save(c)
        }
    }

}

func submit_caption(w http.ResponseWriter, r *http.Request) {
    caption := Caption {
        Gid:        r.FormValue("gid"),
        Aid:        r.FormValue("aid"),
        Caption:    r.FormValue("caption"),
        Language:   r.FormValue("lang"),
        Votes:      1,
    }
    c := appengine.NewContext(r)

    if !assert_gid_exist(c, caption.Gid) {
        fmt.Fprintf(w, "{\"error\":\"gid not found\"}")
        return
    }

    if !assert_aid_exist(c, caption.Aid) {
        fmt.Fprintf(w, "{\"error\":\"aid not found\"}")
        return
    }

    q := datastore.NewQuery("caption").Filter("Gid =", caption.Gid).Filter("Aid =", caption.Aid)
    var captions []Caption
    k, err := q.GetAll(c, &captions)
    if err != nil {
        c.Errorf("Error GetAll Caption for %q: %v\n", caption.Gid, err)
        return
    }
    if len(captions) > 0 {
        captions[0].Id = k[0].IntID()
        captions[0].Caption = r.FormValue("caption")
        captions[0].save(c)
        fmt.Fprintf(w, "{\"status\":\"updated\", \"cid\":\"%s\"}", captions[0].Cid)
    } else {
        user := get_user(c, caption.Gid);
        if user != nil {
            user.Score += 1;
            user.save(c)
        }
        caption.save(c)
        caption.save(c)
        fmt.Fprintf(w, "{\"status\":\"added\", \"cid\":\"%s\"}", caption.Cid)
    }
    key := "captions-" + caption.Gid
    err = memcache.Delete(c, key)
    if err != nil {
        c.Infof("Cannot delete cached item %q: %v\n", key, err)
     }

}

func (t *Caption) key(c appengine.Context) *datastore.Key {
	if t.Id == 0 {
		t.Created = time.Now()
		return datastore.NewIncompleteKey(c, "caption", nil)
	}
	return datastore.NewKey(c, "caption", "", t.Id, nil)
}

func (t *Caption) save(c appengine.Context) (*Caption, error) {
    t.Updated = time.Now()
	k, err := datastore.Put(c, t.key(c), t)
	if err != nil {
		return nil, err
	}
	if k.IntID() != 0 {
        buf := base58.EncodeBig(nil, big.NewInt(k.IntID()))
        t.Id = k.IntID()
        t.Cid = string(buf)
        c.Infof("\t> key is %s\n", t.Id)
    }

    // Create an Item
    key := "caption-" + t.Gid + "-" + t.Aid;
    item := &memcache.Item{
        Key:   key,
        Object: t,
    }
    if err := memcache.Gob.Add(c, item); err == memcache.ErrNotStored {
      c.Infof("item with key %q already exists", item.Key)
    } else if err != nil {
      c.Errorf("error adding item: %v", err)
    } else {
      c.Infof("item with key %q cached", item.Key)
    }

	return t, nil
}

func assert_aid_exist(c appengine.Context, aid string) bool {
    q := datastore.NewQuery("artwork").Filter("Aid =", aid)
    var artworks []Artwork
    _, err := q.GetAll(c, &artworks)
    if err != nil {
        return false;
    }
    return len(artworks) > 0
}

func (t *Vote) key(c appengine.Context) *datastore.Key {
	if t.Id == 0 {
		t.Created = time.Now()
		return datastore.NewIncompleteKey(c, "vote", nil)
	}
	return datastore.NewKey(c, "vote", "", t.Id, nil)
}

func (t *Vote) save(c appengine.Context) (*Vote, error) {
    t.Updated = time.Now()
	k, err := datastore.Put(c, t.key(c), t)
	if err != nil {
		return nil, err
	}
	if k.IntID() != 0 {
        t.Id = k.IntID()
        c.Infof("\t> key is %s\n", t.Id)
    }
	return t, nil
}

func assert_gid_exist(c appengine.Context, gid string) bool {
    q := datastore.NewQuery("user").Filter("Gid =", gid)
    var users []User
    _, err := q.GetAll(c, &users)
    if err != nil {
        return false;
    }
    return len(users) > 0
}

func assert_cid_exist(c appengine.Context, cid string, gid string) bool {
    q := datastore.NewQuery("caption").Filter("Cid =", cid)
    var captions []Caption
    _, err := q.GetAll(c, &captions)
    if err != nil {
        return false;
    }
    return len(captions) > 0 && gid != captions[0].Gid
}
