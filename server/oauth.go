package muzeighibli

import (
    "time"
    "net/http"
    "encoding/json"
    "appengine"
    "appengine/datastore"
    "appengine/memcache"
)

type User struct {
    Id            int64         `json:"id" datastore:"-"`
    Gid           string        `json:"gid"`
    Did           string        `json:"did"`
    Name          string        `json:"name"`
    Language      string        `json:"lang"`
    Role          string        `json:"role"`
    Score         int           `json:"score"`
    Downloads     int           `json:"downloads"`
    Created       time.Time     `json:"-"`
    Updated       time.Time     `json:"-"`
}

func oauth2callback(w http.ResponseWriter, r *http.Request) {
    u := User {
        Gid:        r.FormValue("gid"),
        Did:        r.FormValue("did"),
        Name:       r.FormValue("name"),
        Language:   r.FormValue("lang"),
        Score:      0,
    }
    c := appengine.NewContext(r)


    key := "user-" + u.Gid
    _, err := memcache.Gob.Get(c, key, &u);
    if err == memcache.ErrCacheMiss {
        q := datastore.NewQuery("user").Filter("Gid =", u.Gid)
        var users []User
        keys, err := q.GetAll(c, &users)
        if err != nil {
            c.Errorf("Error GetAll User for %q: %v\n", u.Gid, err)
            return
        }
        if len(users) == 1 {
            u = users[0]
            u.Id = keys[0].IntID()
            u.Name = r.FormValue("name")
            u.Language = r.FormValue("lang")
        }
        u.save(c)
        w.WriteHeader(204)
    } else {
        c.Infof("User is in cache: %q - %q", u.Gid, u.Role)
        if(u.Role == "admin") {
            c.Infof("User is admin")
        }
        enc := json.NewEncoder(w)
        if err := enc.Encode(&u); err != nil {
            http.Error(w, err.Error(), http.StatusInternalServerError)
            return
        }
    }
}



func doauth2callback(w http.ResponseWriter, r *http.Request) {
    u := User {
        Did:        r.FormValue("did"),
        Gid:        r.FormValue("did"),
        Name:       r.FormValue("name"),
        Language:   r.FormValue("lang"),
        Score:      0,
    }
    c := appengine.NewContext(r)


    key := "user-" + u.Did
    _, err := memcache.Gob.Get(c, key, &u);
    if err == memcache.ErrCacheMiss {
        q := datastore.NewQuery("user").Filter("Did =", u.Did)
        var users []User
        keys, err := q.GetAll(c, &users)
        if err != nil {
            c.Errorf("Error GetAll User for %q: %v\n", u.Did, err)
            return
        }
        if len(users) == 1 {
            u = users[0]
            u.Id = keys[0].IntID()
            u.Name = r.FormValue("name")
            u.Language = r.FormValue("lang")
        }
        u.save(c)
        w.WriteHeader(204)
    } else {
        c.Infof("User is in cache: %q - %q", u.Did, u.Role)
        if(u.Role == "admin") {
            c.Infof("User is admin")
        }
        enc := json.NewEncoder(w)
        if err := enc.Encode(&u); err != nil {
            http.Error(w, err.Error(), http.StatusInternalServerError)
            return
        }
    }
}

func get_user(c appengine.Context, gid string) *User {
    var user *User
    q := datastore.NewQuery("user").Filter("Gid =", gid)
    var users []User
    keys, err := q.GetAll(c, &users)
    if err != nil {
        c.Errorf("Error GetAll User for %q: %v\n", gid, err)
    }
    if len(users) == 1 {
        user = &users[0]
        user.Id = keys[0].IntID()
    }
    return user
}

func (t *User) key(c appengine.Context) *datastore.Key {
	if t.Id == 0 {
		t.Created = time.Now()
		return datastore.NewIncompleteKey(c, "user", nil)
	}
	return datastore.NewKey(c, "user", "", t.Id, nil)
}

func (t *User) save(c appengine.Context) (*User, error) {
    t.Updated = time.Now()
	k, err := datastore.Put(c, t.key(c), t)
	if err != nil {
		return nil, err
	}
	t.Id = k.IntID()

	// Create an Item
	key := "user-" + t.Gid
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