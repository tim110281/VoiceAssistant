import pyrebase
import config
firebase = pyrebase.initialize_app(config.config)

# push data into database
db = firebase.database()
data = {"name": "Mortimer 'Morty' Smith"}
db.child("users").push(data)

# fetch data from database
res = db.child("users").get()
print(res.val())
