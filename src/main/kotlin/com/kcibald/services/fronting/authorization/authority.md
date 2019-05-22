# Authority
This file defines how authority is used in the project

## Structure
`
check-type::action(:resource)
`
### check type
depend on user or authority required, this can be any of these value:

| character | word       | description |
|:---------:|:----------:|:-----------:|
|   `p`     | privileged | privileged operation | 
|   `d`     | disallowed | disallowed operation |
|   `c`     | check      | not disallowed (only on request) |

### action
format:
`
catagortay/specific-action
`
#### categories
| character | word       | description  |
|:--------:|:----------:|:-------------:|
|   pos    | post       | post related  | 
|   reg    | region     | region related|
|   upl    | upload     | upload content|

#### specific actions
post:
- `acs`, access, reading/accessing information about the post 
- `mod`, modify, modify its content
- `com/cre`, comment/create, creating new comments under region
- `com/del`, comment/delete, delete comment(s) under a post

region:
- `acs`, access, reading/accessing information about the region
- `mod`, modify, modify the region's information
- `pos/cre`, post/create, creating new post under the region
- `pos/del`, post/delete, delete post under the region

upl:
- `pic`, upload/picture, upload images 
- `vid`, upload/video, upload videos
- `oth`, upload/other, upload type of files (for example pdf, word document, etc)

#### resource
A collection of resources can be represent using
`[resource 1][resource 2]...`

post: 
- general: `regionId/postId`
- comment related: `regionId/postId/commentId`

region:
- general: `regionId`
- post deletion: `regionId/postId`

upl: no resource link will be needed

## User

### Privileged authorization access
- Gets privilege to access post
`
p::pos/acs:tNZLNKTQmh/ph7J9zJe66
`

- Gets privilege to do anything under certain region
`
p::reg/*:doEKvpBKh9
`

### Privileged-ly Disallowed
- Gets banned from all operation:
`
d::*:*
`

- Gets banned from access region:
`
d::reg/*:doEKvpBKh9
`

- Gets banned from access post:
`
d::pos/acs:tNZLNKTQmh/ph7J9zJe66
`

- Gets banned from posting comment under post:
`
d::pos/com/cre:tNZLNKTQmh/ph7J9zJe66
`

## Requirement
### Privilege
- Special privilege to access post
`
p::pos/acs:tNZLNKTQmh/ph7J9zJe66
`
- Not disallowed from access post
`
c::pos/acs:tNZLNKTQmh/ph7J9zJe66
`
- Not disallowed from accessing multiple posts
`
c::pos/acs:[tNZLNKTQmh/ph7J9zJe66][BZbdh1yX3a/RYH6w4Lg1P]
`
