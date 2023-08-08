from django.db import models


class Image(models.Model):
    image = models.ImageField('이미지', upload_to='makeup')

    class Meta:
        db_table = 'images'
