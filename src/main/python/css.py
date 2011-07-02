def generate_body(content):
    """ Wrap content using <body> </body> tags.
    Args:
        content to be enclosed
    Returns:
        html code
    """

    wrapper = """<body>%s</body>"""
    return wrapper%(content,)

def generate_CSSdiv(divbody, classname):
    """ Wraps css div using class name and divbody
    This is a helper function for generate_single_cloud()
    Args:
        divbody: div body to be wrapped around.
        classname: Class to be embedded.
    Return:
        div string
    """

    divstr = """<div class="%s">%s</div>
    """
    return divstr % (classname, divbody)

def generate_style_link(url):
    """ Encloses url in html link tags for use as style sheet
    Args:
        url
    Return:
        html code
    """
    code = """<link href='%s' rel='stylesheet' type = 'text/css' />"""
    return code %(url,)

def generate_single_cloud(clusterid,features):
    """ Generates a single cloud from features.
    Args:
        clusterid: integer identifier for cluster
        features: A dict of features where key corresponds to class id and value
        corresponds to feature to embed. id  should range between [1-10]
    Returns:
        cloud_element :A single css element corresponding to the cloud
    """

    body = """<span class="cluster-id">%d</span>"""%(clusterid,)
    for key,value in features.iteritems():
        assert key > 0 and key <= 10 ,"Key: %d is not in range [1-10]"%(key,)
        body += """<span class="word-%d">%s</span>"""%(key,value)
    return generate_CSSdiv(body,"word-cloud")


def wrap_into_html(body,sessionid,fonturl,styleurl):
    """ Generate html file inserting into head appropriate styles.
    Args:
        body: The body assumed to be enclosed in <body></body>
        sessionid: Used to generate file metadata
        fonturl: Url used to generate font
        styleurl: Url used to generate style
    Return:
        html object
    """
    import datetime
    now = datetime.datetime.today().strftime("%Y%m%d-%H%M%S")
    fontlink = generate_style_link(fonturl)
    stylelink = generate_style_link(styleurl)
    wrapper = """<html>
    <head>
    <title>%s output - %s</title>
    %s
    %s
    <meta charset="utf-8" />
    </head>
    %s
    </html>"""
    whole = wrapper % (sessionid,now,fontlink,stylelink,body)
    return whole






