FROM alpine:3.7 as base
CMD ["echo", "hello base tagged by the stage name"]

FROM base
RUN mkdir /content
COPY ./*.txt /content/
CMD ["cat", "/content/VERSION.txt"]
