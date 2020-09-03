import pyrebase
config = {
	"authDomain": "voiceassistant-424d9.firebaseapp.com",
	"databaseURL": "https://voiceassistant-424d9.firebaseio.com",
	"storageBucket": "voiceassistant-424d9.appspot.com"
}
firebase = pyrebase.initialize_app(config)

# push data into database
db = firebase.database()
data = {"name": "Mortimer 'Morty' Smith"}
db.child("users").push(data)

# fetch data from database
res = db.child("users").get()
print(res.val())
